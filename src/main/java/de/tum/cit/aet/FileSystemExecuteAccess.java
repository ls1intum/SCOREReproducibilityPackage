package de.tum.cit.aet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Encapsulates execution demonstrations for on-disk scripts.
 *
 * <p>Description: Provides helpers that execute shell/batch scripts via
 * different Java process APIs so runtime controls can consistently observe and
 * intercept execution requests.
 *
 * <p>Design Rationale: Consolidating the behavior ensures each execution path
 * is audited and supports platform-specific nuances without scattering logic
 * through the codebase.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
public class FileSystemExecuteAccess extends ProtectedRessourceAccess {

    /**
     * Total amount of execution strategies implemented.
     */
    private static final int AMOUNT_OF_METHODS = 3;

    /**
     * Indicates whether the current runtime targets Windows semantics.
     */
    private static final boolean WINDOWS = System.getProperty("os.name", "")
            .toLowerCase(Locale.ROOT)
            .contains("win");

    /**
     * Reports the number of supported execution methods.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return amount of execution demonstrations
     */
    public static int getAmountOfMethods() {
        return AMOUNT_OF_METHODS;
    }

    /**
     * Lists the command prefix and script path used by each execution method.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return immutable list containing the command prefix and script path
     */
    @Override
    public List<String> listHandeledRessources() {
        if (WINDOWS) {
            return List.of(
                    "cmd /c ",
                    "paths/FileToExecute.bat");
        } else {
            return List.of(
                    "/bin/sh -c ",
                    "paths/FileToExecute.sh");
        }
    }

    /**
     * Generates localized success and failure messages for script execution.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param parameters array containing the path, payload, and optional id
     * @return success and failure message templates
     */
    @Override
    public List<String> getMessages(String[] parameters) {
        String path = "";
        if (parameters.length > 0) {
            path = parameters[0];
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
                String.format("Successfully executed path at %s%s", path, suffix),
                String.format("Failed to execute path at %s for operation id %s", path, id));
    }

    /**
     * Executes the on-disk script with the specified method id.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier of the execution mechanism to use
     * @return formatted message describing the execution result
     * @throws IOException if execution fails or output cannot be captured
     */
    @Override
    public String accessProtectedRessourceById(int id) throws IOException {
        String pathAsString = listHandeledRessources().get(1);
        Path path = Path.of(pathAsString);
        File file = path.toFile();
        if (!Files.exists(path) || !file.canExecute()) {
            return failure(pathAsString, id);
        }
        return switch (id) {
            case 1 -> executeWithRuntimeExec(pathAsString, file);
            case 2 -> executeWithProcessBuilderStart(pathAsString, file);
            case 3 -> executeWithProcessBuilderPipeline(pathAsString, file);
            default -> failure(pathAsString, id);
        };
    }

    /**
     * Concatenates the configured command prefix and script path.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resources list containing the prefix and script path
     * @return combined command ready for execution
     */
    private static String fullCommand(List<String> resources) {
        return String.join("", resources);
    }

    /**
     * Runs the script using {@link Runtime#exec(String[])}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the script path
     * @param file script file handle used for context (unused but kept for parity)
     * @return success message containing the execution output
     * @throws IOException if the runtime call fails
     */
    private String executeWithRuntimeExec(String path, File file) throws IOException {
        Process process = Runtime.getRuntime().exec(fullCommand(listHandeledRessources()).split(" "));
        return success(path, captureProcessResult("Runtime.exec", process));
    }

    /**
     * Runs the script using {@link ProcessBuilder#start()}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the script path
     * @param file script file handle used for context (unused but kept for parity)
     * @return success message containing the execution output
     * @throws IOException if the process cannot be launched
     */
    private String executeWithProcessBuilderStart(String path, File file) throws IOException {
        Process process = new ProcessBuilder(fullCommand(listHandeledRessources())).start();
        return success(path, captureProcessResult("ProcessBuilder.start", process));
    }

    /**
     * Runs the script using a {@link ProcessBuilder#startPipeline(List)} to pipe output.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the script path
     * @param file script file handle used for context (unused but kept for parity)
     * @return success message containing the execution output
     * @throws IOException if the pipeline cannot be created
     */
    private String executeWithProcessBuilderPipeline(String path, File file) throws IOException {
        ProcessBuilder producer = new ProcessBuilder(fullCommand(listHandeledRessources()));
        ProcessBuilder consumer = new ProcessBuilder(WINDOWS ? "cmd /c more" : "/bin/sh -c cat");
        List<Process> pipeline = ProcessBuilder.startPipeline(List.of(producer, consumer));
        return success(path, capturePipelineResult("ProcessBuilder.pipeline", pipeline));
    }
}
