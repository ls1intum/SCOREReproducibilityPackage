package de.tum.cit.aet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the file system write access implementation.
 *
 * <p>Description: Executes each write method id and verifies that success and failure messaging,
 * along with file content, aligns with expectations.
 *
 * <p>Design Rationale: Maintains reproducibility guarantees by exercising every supported write path.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileSystemWriteAccessRunner implements TestResourceLifecycle {

    /**
     * Default content written before running each scenario.
     */
    private static final String INITIAL_CONTENT = "initial-write-content";

    /**
     * Fully qualified name of the write access implementation.
     */
    private static final String ACCESS_CLASS = "de.tum.cit.aet.FileSystemWriteAccess";

    /**
     * Access instance created via reflection.
     */
    private Object access;

    /**
     * Paths managed by the write access class.
     */
    private List<Path> resourcePaths;

    /**
     * Resets the test resources before each case.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws Exception if reflection or I/O fails
     */
    @BeforeEach
    @Override
    public void setupRessources() throws Exception {
        access = ReflectionAccessHelper.instantiate(ACCESS_CLASS);
        resourcePaths = ReflectionAccessHelper.listHandledResources(access).stream()
                .map(Path::of)
                .collect(Collectors.toList());
        writeInitialContent();
    }

    /**
     * Restores the initial content after each case.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws IOException if resetting fails
     */
    @AfterEach
    @AfterEach
    @Override
    public void resetRessources() throws IOException {
        writeInitialContent();
    }

    /**
     * Validates each write method id.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier provided by the method source
     * @return nothing, assertions enforce expectations
     */
    @ParameterizedTest(name = "accessProtectedRessourceById({0})")
    @MethodSource("methodIds")
    @Override
    public void testRessourceAccess(int id) {
        ReflectionAccessHelper.InvocationResult result = ReflectionAccessHelper.invokeAndCapture(access, id);

        if (result.succeeded()) {
            assertTrue(result.message().startsWith("Successfully written resource"),
                    () -> "Expected success for id " + id + " but got: " + result.message());
        } else {
            assertTrue(result.message().startsWith("Failed to write resource") || result.exception() instanceof Exception,
                    () -> "Expected failure for unsupported id " + id + " but got: " + result.message());
        }

        byte[] initialBytes = INITIAL_CONTENT.getBytes(StandardCharsets.UTF_8);
        for (Path path : resourcePaths) {
            assertTrue(Files.exists(path), () -> "Expected file to exist after write: " + path);
            byte[] contents;
            try {
                contents = Files.readAllBytes(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file " + path, e);
            }
            if (result.succeeded()) {
                assertTrue(contents.length > 0, () -> "Expected non-empty content for " + path);
            } else {
                assertTrue(Arrays.equals(initialBytes, contents),
                        () -> "Unsupported id should not change content for " + path);
            }
        }
    }

    /**
     * Writes the initial content to every resource file.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws IOException if writing fails
     * @return nothing
     */
    private void writeInitialContent() throws IOException {
        for (Path path : resourcePaths) {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, INITIAL_CONTENT, StandardCharsets.UTF_8);
        }
    }

    /**
     * Provides method ids to the parameterized test.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return stream spanning every supported method id
     */
    @Override
    public IntStream methodIds() {
        return IntStream.rangeClosed(1, ReflectionAccessHelper.getAmountOfMethods(ACCESS_CLASS));
    }
}
