package de.tum.cit.aet;

/**
 * Runnable used for demonstrating reflective thread creation flows.
 *
 * <p>Description: Implements a lightweight Runnable that prints a
 * message so helper classes can instantiate and execute it via various thread
 * management APIs.
 *
 * <p>Design Rationale: Provides a deterministic, side-effect free workload that
 * proves thread creation succeeded without introducing additional
 * dependencies.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
public class RunnableToCreate implements Runnable {

    /**
     * Emits a simple log entry when executed.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     */
    @Override
    public void run() {
        System.out.println("Runnable is running");
    }
}
