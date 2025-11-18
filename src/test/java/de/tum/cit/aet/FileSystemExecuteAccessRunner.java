package de.tum.cit.aet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the file system execute access implementation.
 *
 * <p>Description: Validates that each configured execution method succeeds or fails with expected
 * messaging while leaving the configured script intact.
 *
 * <p>Design Rationale: Provides confidence that the reproducibility package covers command execution
 * restrictions consistently across platforms.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FileSystemExecuteAccessRunner implements TestResourceLifecycle {

    /**
     * Indicates whether the environment is Windows to choose the right script content.
     */
    private static final boolean WINDOWS = System.getProperty("os.name", "")
            .toLowerCase()
            .contains("win");

    /**
     * Fully qualified name of the execute access class.
     */
    private static final String ACCESS_CLASS = "de.tum.cit.aet.FileSystemExecuteAccess";

    /**
     * Access instance created reflectively.
     */
    private Object access;

    /**
     * Path to the executable configured by the access class.
     */
    private Path executable;

    /**
     * Instantiates the access class and prepares the executable file.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws Exception if reflection or file creation fails
     */
    @BeforeEach
    @Override
    public void setupRessources() throws Exception {
        access = ReflectionAccessHelper.instantiate(ACCESS_CLASS);
        executable = Path.of(ReflectionAccessHelper.listHandledResources(access).get(1));
        setupExecutable();
    }

    /**
     * Cleans up the script after each scenario.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws IOException if deletion fails
     */
    @AfterEach
    @Override
    public void resetRessources() throws IOException {
        Files.deleteIfExists(executable);
    }

    /**
     * Ensures each execution method id behaves as documented.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier supplied by the method source
     * @return nothing, assertions enforce correctness
     */
    @ParameterizedTest(name = "accessProtectedRessourceById({0})")
    @MethodSource("methodIds")
    @Override
    public void testRessourceAccess(int id) {
        ReflectionAccessHelper.InvocationResult result = ReflectionAccessHelper.invokeAndCapture(access, id);
        boolean supported = isSupported(id);

        if (!supported) {
            assertTrue(!result.succeeded() && result.message().startsWith("Failed to execute path"),
                    () -> "Expected failure for unsupported id " + id + " but got: " + result.message());
            return;
        }

        if (result.succeeded()) {
            assertTrue(result.message().startsWith("Successfully executed path"),
                    () -> "Expected success for id " + id + " but got: " + result.message());
        } else {
            assertTrue(result.message().startsWith("Failed to execute path") || result.exception() instanceof IOException,
                    () -> "Supported id " + id + " failed unexpectedly: " + result.message());
        }

        assertTrue(Files.exists(executable), "Executable should remain present");
        assertTrue(Files.isExecutable(executable), "Executable permissions should remain set");
    }

    /**
     * Creates the executable script with platform-appropriate content.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws IOException if writing fails
     * @return nothing
     */
    private void setupExecutable() throws IOException {
        Path parent = executable.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String script = WINDOWS
                ? "@echo off" + System.lineSeparator() + "echo Hello FileSystemExecuteAccess"
                : "#!/bin/sh" + System.lineSeparator() + "echo Hello FileSystemExecuteAccess";
        Files.writeString(executable, script, StandardCharsets.UTF_8);
        executable.toFile().setExecutable(true);
    }

    /**
     * Determines whether the provided id is supported.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier to evaluate
     * @return {@code true} when within the supported range
     */
    private static boolean isSupported(int id) {
        return id >= 1 && id <= ReflectionAccessHelper.getAmountOfMethods(ACCESS_CLASS);
    }

    /**
     * Supplies all method ids to the parameterized test.
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
