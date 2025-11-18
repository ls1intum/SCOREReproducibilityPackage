package de.tum.cit.aet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the network receive access implementation.
 *
 * <p>Description: Exercises each receive method id and validates success and failure messaging for
 * TCP and UDP flows.
 *
 * <p>Design Rationale: Keeps the reproducibility package honest by ensuring receive operations behave
 * consistently across environments.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NetworkSystemReceiveAccessRunner implements TestResourceLifecycle {

    /**
     * Fully qualified name of the receive access class.
     */
    private static final String ACCESS_CLASS = "de.tum.cit.aet.NetworkSystemReceiveAccess";

    /**
     * Access instance created before each test.
     */
    private Object access;

    /**
     * Instantiates the receive access class.
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
     * Placeholder cleanup hook for networking resources.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     */
    @AfterEach
    @Override
    public void resetRessources() {
        // network resources auto-close
    }

    /**
     * Validates the outcome for each receive method id.
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
            assertTrue(!result.succeeded(),
                    () -> "Expected failure for unsupported id " + id + " but got: " + result.message());
            return;
        }

        if (result.succeeded()) {
            assertTrue(result.message().startsWith("Successfully received data"),
                    () -> "Expected success for id " + id + " but got: " + result.message());
        } else {
            assertTrue(result.message().startsWith("Failed to receive data") || result.exception() instanceof Exception,
                    () -> "Supported id " + id + " failed unexpectedly: " + result.message());
        }
    }

    /**
     * Indicates whether the supplied id is supported.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier to evaluate
     * @return {@code true} when the id is within range
     */
    private static boolean isSupported(int id) {
        return id >= 1 && id <= ReflectionAccessHelper.getAmountOfMethods(ACCESS_CLASS);
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
