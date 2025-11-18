package de.tum.cit.aet;

/**
 * Thread subclass instantiated during thread creation demonstrations.
 *
 * <p>Description: Supplies a concrete Thread implementation that emits
 * a log message when started so reflective instantiation helpers have a known
 * target.
 *
 * <p>Design Rationale: Keeps the runnable workload self-contained while still
 * verifying that helper methods successfully created and executed a thread
 * instance.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
public class ThreadToCreate extends Thread {

    /**
     * Writes a status message when the thread runs.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     */
    @Override
    public void run() {
        System.out.println("Thread is running");
    }
}
