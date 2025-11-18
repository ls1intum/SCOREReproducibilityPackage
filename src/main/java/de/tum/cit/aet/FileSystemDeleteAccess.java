package de.tum.cit.aet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Houses demonstrations for deleting files through various APIs.
 *
 * <p>Description: Provides multiple delete strategies ranging from legacy file
 * methods to modern Files helpers to cover the most common
 * programmatic behaviors that runtime policies must monitor.
 *
 * <p>Design Rationale: Keeping each delete variation in one place simplifies
 * the process of auditing and expanding the supported methods over time.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
public class FileSystemDeleteAccess extends ProtectedRessourceAccess {

    /**
     * Number of delete implementation variants available in this class.
     */
    private static final int AMOUNT_OF_METHODS = 10;

    /**
     * Reports the amount of supported delete methods.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return total number of delete demonstrations
     */
    public static int getAmountOfMethods() {
        return AMOUNT_OF_METHODS;
    }

    /**
     * Lists the resources targeted by the delete demonstrations.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return immutable list containing the file slated for deletion
     */
    @Override
    public List<String> listHandeledRessources() {
        return List.of("resources/FileToDelete.txt");
    }

    /**
     * Formats human-readable messages for delete attempts.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param parameters array containing the path, payload, and optional id
     * @return success and failure messages for delete operations
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
                String.format("Successfully deleted file at path: %s%s", path, suffix),
                String.format("Failed to delete resource at %s for operation id %s", path, id));
    }

    /**
     * Executes the requested delete method identified by id.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier of the delete strategy to run
     * @return textual representation of the delete result
     * @throws IOException if the delete operation fails
     */
    @Override
    public String accessProtectedRessourceById(int id) throws IOException {
        String pathAsString = listHandeledRessources().get(0);
        Path path = Path.of(pathAsString);
        File file = path.toFile();
        if (!Files.exists(path) || !file.canWrite()) {
            return failure(pathAsString, id);
        }
        return switch (id) {
            case 1 -> deleteWithFileDelete(pathAsString, file);
            case 2 -> deleteWithFileDeleteOnExit(pathAsString, file);
            case 3 -> deleteWithFilesDelete(pathAsString, file);
            case 4 -> deleteWithFilesDeleteIfExists(pathAsString, file);
            case 5 -> deleteWithOutputStream(pathAsString, file);
            case 6 -> deleteWithBufferedWriter(pathAsString, file);
            case 7 -> deleteWithSeekableByteChannel(pathAsString, file);
            case 8 -> deleteWithFileChannel(pathAsString, file);
            case 9 -> deleteWithFilesWriteBytes(pathAsString, file);
            case 10 -> deleteWithFilesWriteString(pathAsString, file);
            default -> failure(pathAsString, id);
        };
    }

    /**
     * Deletes the file while closing an output stream obtained from {@link Files#newOutputStream(Path, java.nio.file.OpenOption...)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the path being deleted
     * @param file handle for compatibility checks
     * @return success message listing the stream used
     * @throws IOException if the stream cannot be opened
     */
    private String deleteWithOutputStream(String path, File file) throws IOException {
        try (OutputStream out = Files.newOutputStream(file.toPath(), StandardOpenOption.DELETE_ON_CLOSE)) {
            return success(path, out.getClass().getSimpleName());
        }
    }

    /**
     * Deletes the file while closing a buffered writer created via {@link Files#newBufferedWriter(Path, java.nio.charset.Charset, java.nio.file.OpenOption...)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the path being deleted
     * @param file handle for compatibility checks
     * @return success message listing the writer used
     * @throws IOException if the writer cannot be opened
     */
    private String deleteWithBufferedWriter(String path, File file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
                StandardOpenOption.DELETE_ON_CLOSE)) {
            return success(path, writer.getClass().getSimpleName());
        }
    }

    /**
     * Deletes the file by closing a byte channel opened via {@link Files#newByteChannel(Path, java.nio.file.OpenOption...)} in delete-on-close mode.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the path being deleted
     * @param file handle for compatibility checks
     * @return success message listing the channel used
     * @throws IOException if the channel cannot be opened
     */
    private String deleteWithSeekableByteChannel(String path, File file) throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(file.toPath(), StandardOpenOption.DELETE_ON_CLOSE)) {
            return success(path, channel.getClass().getSimpleName());
        }
    }

    /**
     * Deletes the file by closing {@link FileChannel#open(Path, java.nio.file.OpenOption...)} with delete-on-close semantics.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the path being deleted
     * @param file handle for compatibility checks
     * @return success message listing the channel used
     * @throws IOException if the channel cannot be opened
     */
    private String deleteWithFileChannel(String path, File file) throws IOException {
        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.DELETE_ON_CLOSE)) {
            return success(path, channel.getClass().getSimpleName());
        }
    }

    /**
     * Deletes the file using {@link File#delete()}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the path being deleted
     * @param file handle slated for deletion
     * @return success message containing the boolean delete result
     */
    private String deleteWithFileDelete(String path, File file) {
        return success(path, Boolean.toString(file.delete()));
    }

    /**
     * Schedules the file for deletion on JVM exit via {@link File#deleteOnExit()}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the path being deleted
     * @param file handle slated for deletion
     * @return success message confirming the scheduling request
     */
    private String deleteWithFileDeleteOnExit(String path, File file) {
        file.deleteOnExit();
        return success(path, "");
    }

    /**
     * Deletes the file using {@link Files#delete(Path)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the path being deleted
     * @param file handle slated for deletion
     * @return success message confirming the delete call
     * @throws IOException if the file cannot be deleted
     */
    private String deleteWithFilesDelete(String path, File file) throws IOException {
        Files.delete(file.toPath());
        return success(path, "");
    }

    /**
     * Deletes the file using {@link Files#deleteIfExists(Path)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the path being deleted
     * @param file handle slated for deletion
     * @return success message containing the boolean delete result
     * @throws IOException if the delete fails due to I/O errors
     */
    private String deleteWithFilesDeleteIfExists(String path, File file) throws IOException {
        return success(path, Boolean.toString(Files.deleteIfExists(file.toPath())));
    }

    /**
     * Deletes the file by writing an empty payload with delete-on-close semantics.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the path being deleted
     * @param file handle slated for deletion
     * @return success message containing the resulting path
     * @throws IOException if the write fails
     */
    private String deleteWithFilesWriteBytes(String path, File file) throws IOException {
        return success(path, Files.write(file.toPath(), new byte[0], StandardOpenOption.DELETE_ON_CLOSE).toString());
    }

    /**
     * Deletes the file by writing an empty string while using delete-on-close semantics.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the path being deleted
     * @param file handle slated for deletion
     * @return success message containing the resulting path
     * @throws IOException if the write fails
     */
    private String deleteWithFilesWriteString(String path, File file) throws IOException {
        return success(path, Files.writeString(file.toPath(), "", StandardOpenOption.DELETE_ON_CLOSE).toString());
    }
}
