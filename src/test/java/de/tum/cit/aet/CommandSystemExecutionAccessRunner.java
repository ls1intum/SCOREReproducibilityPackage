package de.tum.cit.aet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the command system execution access implementation.
 *
 * <p>Description: Executes each command method id and verifies the resulting messages and exception
 * handling.
 *
 * <p>Design Rationale: Ensures command execution demos remain accurate and predictable.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CommandSystemExecutionAccessRunner implements TestResourceLifecycle {

    /**
     * Fully qualified name of the access class.
     */
    private static final String ACCESS_CLASS = "de.tum.cit.aet.CommandSystemExecutionAccess";

    /**
     * Access instance created before each test.
     */
    private Object access;

    /**
     * Instantiates the command access class.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws Exception if reflection fails
     */
    @BeforeEach
    @Override
    public void setupRessources() throws Exception {
        access = ReflectionAccessHelper.instantiate(ACCESS_CLASS);
    }

    /**
     * Placeholder for potential cleanup steps.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     */
    @AfterEach
    @Override
    public void resetRessources() {
        // no shared resources to reset
    }

    /**
     * Validates each command execution method id.
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
        boolean supported = isSupported(id);

        if (!supported) {
            assertTrue(!result.succeeded() && result.message().startsWith("Failed to execute command"),
                    () -> "Expected failure for unsupported id " + id + " but got: " + result.message());
            return;
        }

        if (result.succeeded()) {
            assertTrue(result.message().startsWith("Successfully executed command"),
                    () -> "Expected successful execution for id " + id + " but got: " + result.message());
        } else {
            assertTrue(result.message().startsWith("Failed to execute command") || result.exception() instanceof IOException,
                    () -> "Supported id " + id + " failed unexpectedly: " + result.message());
        }
    }

    /**
     * Indicates whether the supplied id is within the supported range.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier to evaluate
     * @return {@code true} when in range
     */
    private static boolean isSupported(int id) {
        int max = ReflectionAccessHelper.getAmountOfMethods(ACCESS_CLASS);
        return id >= 1 && id <= max;
    }

    /**
     * Supplies method ids to the parameterized test.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return stream covering every supported id
     */
    @Override
    public IntStream methodIds() {
        return IntStream.rangeClosed(1, ReflectionAccessHelper.getAmountOfMethods(ACCESS_CLASS));
    }
}
