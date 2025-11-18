package de.tum.cit.aet;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Demonstrates the various ways to execute operating system commands.
 *
 * <p>Description: Provides sample invocations that exercise different Java
 * process execution APIs so access to command execution can be validated or
 * blocked consistently.
 *
 * <p>Design Rationale: Consolidates command execution helpers inside a single
 * access class to simplify auditing and to make the supported methods explicit.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
public class CommandSystemExecutionAccess extends ProtectedRessourceAccess {

    /**
     * Total number of command execution variants implemented by this class.
     */
    private static final int AMOUNT_OF_METHODS = 3;

    /**
     * Indicates whether the current runtime executes on a Windows platform.
     */
    private static final boolean WINDOWS = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT)
            .contains("win");

    /**
     * Reports the number of supported execution methods.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return total available execution methods
     */
    public static int getAmountOfMethods() {
        return AMOUNT_OF_METHODS;
    }

    /**
     * Lists the commands that are executed for demonstration purposes.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return command definitions and optional consumers that should be executed
     */
    @Override
    public List<String> listHandeledRessources() {
        if (WINDOWS) {
            return List.of(
                    "cmd /c echo Hello World!",
                    "cmd /c more");
        } else {
            return List.of(
                    "/bin/sh -c echo Hello World!",
                    "/bin/sh -c cat");
        }
    }

    /**
     * Builds the localized success and failure messages for the access methods.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param parameters contextual data such as the command, payload, and id
     * @return immutable list containing the success and failure message text
     */
    @Override
    public List<String> getMessages(String[] parameters) {
        String command = "";
        if (parameters.length > 0) {
            command = parameters[0];
        }
        String payload = "";
        if (parameters.length > 1) {
            payload = parameters[1];
        }
        String id = "";
        if (parameters.length > 2) {
            id = parameters[2];
        }
        String suffix = "";
        if (!payload.isEmpty()) {
            suffix = String.format(" Result: %s", payload);
        }
        return List.of(
                String.format("Successfully executed command at %s%s", command, suffix),
                String.format("Failed to execute command at %s for operation id %s", command, id));
    }

    /**
     * Executes the configured command via the requested method id.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier of the execution mechanism to invoke
     * @return formatted status message describing the execution result
     * @throws IOException if the underlying execution API fails
     */
    @Override
    public String accessProtectedRessourceById(int id) throws IOException {
        String resource = listHandeledRessources().get(0);
        return switch (id) {
            case 1 -> executeWithRuntimeExec(resource);
            case 2 -> executeWithProcessBuilderStart(resource);
            case 3 -> executeWithProcessBuilderPipeline(resource);
            default -> failure(resource, id);
        };
    }

    /**
     * Executes the command using {@link Runtime#exec(String[])}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the command being executed
     * @return success message that contains the execution output
     * @throws IOException if the process cannot be launched or inspected
     */
    private String executeWithRuntimeExec(String resource) throws IOException {
        Process process = Runtime.getRuntime().exec(listHandeledRessources().get(0).split(" "));
        return success(resource, captureProcessResult("Runtime.exec", process));
    }

    /**
     * Executes the command through {@link ProcessBuilder#start()}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the command being executed
     * @return success message that contains the execution output
     * @throws IOException if the process cannot be launched or inspected
     */
    private String executeWithProcessBuilderStart(String resource) throws IOException {
        Process process = new ProcessBuilder(listHandeledRessources().get(0)).start();
        return success(resource, captureProcessResult("ProcessBuilder.start", process));
    }

    /**
     * Executes the command via a {@link ProcessBuilder#startPipeline(List)} chain.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the command being executed
     * @return success message that contains the execution output
     * @throws IOException if the processes cannot be launched or inspected
     */
    private String executeWithProcessBuilderPipeline(String resource) throws IOException {
        ProcessBuilder producer = new ProcessBuilder(listHandeledRessources().get(0));
        ProcessBuilder consumer = new ProcessBuilder(listHandeledRessources().get(1));
        List<Process> processes = ProcessBuilder.startPipeline(List.of(producer, consumer));
        return success(resource, capturePipelineResult("ProcessBuilder.pipeline", processes));
    }
}
