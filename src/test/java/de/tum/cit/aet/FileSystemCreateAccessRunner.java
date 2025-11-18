package de.tum.cit.aet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
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
 * Tests the file system create access implementation end-to-end.
 *
 * <p>Description: Validates that every configured method either creates the expected files or reports
 * a meaningful failure while leaving the file system unchanged.
 *
 * <p>Design Rationale: Ensures the reproducibility package remains verified across platforms by
 * executing every supported method id through reflection.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileSystemCreateAccessRunner implements TestResourceLifecycle {

    /**
     * Fully qualified name of the access class under test.
     */
    private static final String ACCESS_CLASS = "de.tum.cit.aet.FileSystemCreateAccess";

    /**
     * Access instance created via reflection before each test.
     */
    private Object access;

    /**
     * Paths to every resource returned by {@code listHandeledRessources()}.
     */
    private List<Path> resourcePaths;

    /**
     * Configures the access instance and ensures resources are in a clean state.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws Exception if reflection fails during instantiation
     */
    @BeforeEach
    @Override
    public void setupRessources() throws Exception {
        access = ReflectionAccessHelper.instantiate(ACCESS_CLASS);
        resourcePaths = ReflectionAccessHelper.listHandledResources(access).stream()
                .map(Path::of)
                .collect(Collectors.toList());
        ensureParentDirectories();
        resetRessources();
    }

    /**
     * Cleans up test artifacts after each scenario.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws IOException if resource deletion fails
     */
    @AfterEach
    @Override
    public void resetRessources() throws IOException {
        for (Path path : resourcePaths) {
            Files.deleteIfExists(path);
        }
    }

    /**
     * Verifies that every supported id results in the documented behavior.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier supplied by the method source
     * @return nothing, assertions ensure success or failure
     */
    @ParameterizedTest(name = "accessProtectedRessourceById({0})")
    @MethodSource("methodIds")
    @Override
    public void testRessourceAccess(int id) {
        ReflectionAccessHelper.InvocationResult result = ReflectionAccessHelper.invokeAndCapture(access, id);

        if (result.succeeded()) {
            assertTrue(result.message().startsWith("Successfully created resource"),
                    () -> "Unexpected success message for id " + id + ": " + result.message());
        } else {
            assertTrue(result.message().startsWith("Failed to create resource") || result.exception() instanceof Exception,
                    () -> "Expected failure indication for id " + id);
        }

        for (Path path : resourcePaths) {
            boolean exists = Files.exists(path);
            if (result.succeeded()) {
                assertTrue(exists, () -> "File should exist after successful invocation: " + path);
            } else {
                assertFalse(exists, () -> "File should not exist after failed invocation: " + path);
            }
        }
    }

    /**
     * Creates missing parent directories for every resource path.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws IOException if directory creation fails
     * @return nothing
     */
    private void ensureParentDirectories() throws IOException {
        for (Path path : resourcePaths) {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        }
    }

    /**
     * Provides the method ids that should be executed in the parameterized test.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return stream ranging across every supported method id
     */
    /**
     * Provides the method ids that should be executed in the parameterized test.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return stream ranging across every supported method id
     */
    @Override
    public IntStream methodIds() {
        return IntStream.rangeClosed(1, ReflectionAccessHelper.getAmountOfMethods(ACCESS_CLASS));
    }
}
