package de.tum.cit.aet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the file system delete access implementation.
 *
 * <p>Description: Executes every configured delete method and verifies that files are removed (or
 * preserved) according to the documented semantics.
 *
 * <p>Design Rationale: Protects the reproducibility package by ensuring destructive operations behave
 * deterministically across platforms.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileSystemDeleteAccessRunner implements TestResourceLifecycle {

    /**
     * Fully qualified name of the delete access class under test.
     */
    private static final String ACCESS_CLASS = "de.tum.cit.aet.FileSystemDeleteAccess";

    /**
     * Access instance created via reflection.
     */
    private Object access;

    /**
     * Resource paths managed by the delete access implementation.
     */
    private List<Path> resourcePaths;

    /**
     * Creates the access instance and ensures files exist before each test.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws Exception if reflection fails
     */
    @BeforeEach
    @Override
    public void setupRessources() throws Exception {
        access = ReflectionAccessHelper.instantiate(ACCESS_CLASS);
        resourcePaths = ReflectionAccessHelper.listHandledResources(access).stream()
                .map(Path::of)
                .collect(Collectors.toList());
        seedResources();
    }

    /**
     * Restores resources after each test run.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws IOException if resource creation fails
     */
    @AfterEach
    @Override
    public void resetRessources() throws IOException {
        seedResources();
    }

    /**
     * Verifies the reported outcome for every delete method id.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier supplied by the method source
     * @return nothing, assertions cover correctness
     */
    @ParameterizedTest(name = "accessProtectedRessourceById({0})")
    @MethodSource("methodIds")
    @Override
    public void testRessourceAccess(int id) {
        ReflectionAccessHelper.InvocationResult result = ReflectionAccessHelper.invokeAndCapture(access, id);

        if (result.succeeded()) {
            assertTrue(result.message().startsWith("Successfully deleted file"),
                    () -> "Unexpected success message for id " + id + ": " + result.message());
        } else {
            assertTrue(result.message().startsWith("Failed to delete resource") || result.exception() instanceof Exception,
                    () -> "Expected failure indication for id " + id);
        }

        for (Path path : resourcePaths) {
            boolean exists = Files.exists(path);
            if (result.succeeded()) {
                if (id == 2) {
                    // deleteOnExit only removes the file when the JVM terminates; it should still exist now.
                    assertTrue(exists, () -> "deleteOnExit should keep file until JVM exit");
                } else {
                    assertFalse(exists, () -> "File should be removed immediately for id " + id);
                }
            } else {
                assertTrue(exists, () -> "Failures should leave file intact for id " + id);
            }
        }
    }

    /**
     * Writes the seed data to all resources.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws IOException if file creation fails
     */
    private void seedResources() throws IOException {
        for (Path path : resourcePaths) {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, "seed data", StandardCharsets.UTF_8);
        }
    }

    /**
     * Provides all supported method ids for parameterized execution.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return stream spanning every supported id
     */
    /**
     * Provides all supported method ids for parameterized execution.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return stream spanning every supported id
     */
    @Override
    public IntStream methodIds() {
        return IntStream.rangeClosed(1, ReflectionAccessHelper.getAmountOfMethods(ACCESS_CLASS));
    }
}
