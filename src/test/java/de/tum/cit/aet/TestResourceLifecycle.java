package de.tum.cit.aet;

import java.util.stream.IntStream;

/**
 * Contract for tests that prepare, reset, and enumerate resource-based scenarios.
 *
 * <p>Description: Declares lifecycle hooks, parameterized test signatures, and method-id streams so
 * resource-heavy tests follow a consistent structure.
 *
 * <p>Design Rationale: Provides a unified naming convention simplifying maintenance across test
 * suites that interact with the reproducibility package.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
interface TestResourceLifecycle {

    /**
     * Prepares the resources required by the test.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws Exception if preparation fails
     */
    void setupRessources() throws Exception;

    /**
     * Resets or cleans up all previously prepared resources.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @throws Exception if cleanup fails
     */
    void resetRessources() throws Exception;

    /**
     * Supplies the method ids used for parameterized execution.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return stream covering the supported method ids
     */
    IntStream methodIds();

    /**
     * Executes the parameterized resource access scenario.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id method identifier supplied by the stream
     * @return nothing
     */
    void testRessourceAccess(int id);
}
