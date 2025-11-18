package de.tum.cit.aet;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the thread system create access implementation.
 *
 * <p>Description: Runs every thread creation strategy and validates success and failure messaging.
 *
 * <p>Design Rationale: Ensures the reproducibility package accurately reflects JVM thread creation
 * behaviors.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ThreadSystemCreateAccessRunner implements TestResourceLifecycle {

    /**
     * Fully qualified name of the thread access class.
     */
    private static final String ACCESS_CLASS = "de.tum.cit.aet.ThreadSystemCreateAccess";

    /**
     * Access instance created before each test.
     */
    private Object access;

    /**
     * Instantiates the thread access class.
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
     * Placeholder for potential cleanup logic.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     */
    @AfterEach
    @Override
    public void resetRessources() {
        // threads are short lived and already joined
    }

    /**
     * Validates each thread creation method id.
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
            assertTrue(!result.succeeded() && result.message().startsWith("Failed to trigger thread creation"),
                    () -> "Expected failure for unsupported id " + id + " but got: " + result.message());
            return;
        }

        if (result.succeeded()) {
            assertTrue(result.message().startsWith("Successfully triggered thread creation"),
                    () -> "Expected success for id " + id + " but got: " + result.message());
        } else {
            assertTrue(result.message().startsWith("Failed to trigger thread creation") || result.exception() instanceof Exception,
                    () -> "Supported id " + id + " failed unexpectedly: " + result.message());
        }
    }

    /**
     * Indicates whether the supplied id is supported.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier to evaluate
     * @return {@code true} when in range
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
