package de.tum.cit.aet;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Tests the file system read access implementation via reflection.
 *
 * <p>Description: Ensures every read method reports the correct message and leaves the source files
 * untouched, confirming safe reproducibility of read operations.
 *
 * <p>Design Rationale: Validates the reproducibility package by exercising all configured method ids
 * and comparing their outcomes to expectations.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileSystemReadAccessRunner implements TestResourceLifecycle {

    /**
     * Payload written to each resource before tests.
     */
    private static final String PAYLOAD = "read-access-payload";

    /**
     * Fully qualified name of the read access class.
     */
    private static final String ACCESS_CLASS = "de.tum.cit.aet.FileSystemReadAccess";

    /**
     * Access instance created via reflection.
     */
    private Object access;

    /**
     * Handled resource paths.
     */
    private List<Path> resourcePaths;

    /**
     * Instantiates the access class and prepares files before each run.
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
        writePayload();
    }

    /**
     * Removes temporary files after each scenario.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws IOException if deletion fails
     */
    @AfterEach
    @Override
    public void resetRessources() throws IOException {
        for (Path path : resourcePaths) {
            Files.deleteIfExists(path);
        }
    }

    /**
     * Verifies each read method result and ensures resources remain unchanged.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier supplied by the method source
     * @return nothing, relies on assertions
     */
    @ParameterizedTest(name = "accessProtectedRessourceById({0})")
    @MethodSource("methodIds")
    @Override
    public void testRessourceAccess(int id) {
        ReflectionAccessHelper.InvocationResult result = ReflectionAccessHelper.invokeAndCapture(access, id);

        if (result.succeeded()) {
            assertTrue(result.message().startsWith("Successfully read resource"),
                    () -> "Expected success for id " + id + " but got: " + result.message());
        } else {
            assertTrue(result.message().startsWith("Failed to read resource") || result.exception() instanceof Exception,
                    () -> "Expected failure for id " + id + " but got: " + result.message());
        }

        for (Path path : resourcePaths) {
            assertTrue(Files.exists(path), () -> "Expected file to remain for id " + id + ": " + path);
            try {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                assertEquals(PAYLOAD, content, () -> "Content should remain unchanged for " + path);
            } catch (IOException e) {
                assertTrue(false, () -> "Failed to read file content for " + path + ": " + e.getMessage());
            }
        }
    }

    /**
     * Creates resource files populated with the expected payload.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws IOException if writing fails
     * @return nothing
     */
    private void writePayload() throws IOException {
        for (Path path : resourcePaths) {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, PAYLOAD, StandardCharsets.UTF_8);
        }
    }

    /**
     * Supplies method ids to the parameterized test.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return stream covering every supported method id
     */
    @Override
    public IntStream methodIds() {
        return IntStream.rangeClosed(1, ReflectionAccessHelper.getAmountOfMethods(ACCESS_CLASS));
    }
}
