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
 * Encapsulates the demonstrations that create files on disk.
 *
 * <p>Description: Offers multiple creation strategies ranging from legacy
 * File APIs to newer Files helpers so safeguards can block or
 * allow file creation consistently across the stack.
 *
 * <p>Design Rationale: Centralizing the implementations avoids duplication and
 * keeps the list of create operations auditable in a single location.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
public class FileSystemCreateAccess extends ProtectedRessourceAccess {

    /**
     * Number of distinct file creation strategies implemented by this class.
     */
    private static final int AMOUNT_OF_METHODS = 10;

    /**
     * Reports the number of supported file creation methods.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return total amount of file creation demonstrations
     */
    public static int getAmountOfMethods() {
        return AMOUNT_OF_METHODS;
    }

    /**
     * Lists the resource paths targeted by the creation methods.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return immutable list containing the path to create
     */
    @Override
    public List<String> listHandeledRessources() {
        return List.of("resources/FileToCreate.txt");
    }

    /**
     * Assembles localized messages for file creation attempts.
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
                String.format("Successfully created resource at %s%s", path, suffix),
                String.format("Failed to create resource at %s for operation id %s", path, id));
    }

    /**
     * Executes the requested file creation method by id.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier of the creation strategy to execute
     * @return textual description of the creation result
     * @throws IOException if the file cannot be created or inspected
     */
    @Override
    public String accessProtectedRessourceById(int id) throws IOException {
        String pathAsString = listHandeledRessources().get(0);
        Path path = Path.of(pathAsString);
        File file = path.toFile();

        if (!isSupportedMethodId(id, AMOUNT_OF_METHODS)) {
            return failure(pathAsString, id);
        }

        if (Files.exists(path)) {
            return failure(pathAsString, id);
        }

        return switch (id) {
            case 1 -> createWithFileCreateNew(pathAsString, file);
            case 2 -> createWithFileCreateTemp(pathAsString, file);
            case 3 -> createWithFilesCreateFile(pathAsString, file);
            case 4 -> createWithFilesCreateTempFile(pathAsString, file);
            case 5 -> createWithOutputStream(pathAsString, file);
            case 6 -> createWithBufferedWriter(pathAsString, file);
            case 7 -> createWithSeekableByteChannel(pathAsString, file);
            case 8 -> createWithFileChannel(pathAsString, file);
            case 9 -> createWithFilesWriteBytes(pathAsString, file);
            case 10 -> createWithFilesWriteString(pathAsString, file);
            default -> failure(pathAsString, id);
        };
    }

    /**
     * Creates the file using {@link Files#newOutputStream(Path, java.nio.file.OpenOption...)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file handle used for platform checks
     * @return success message describing the stream used
     * @throws IOException if the output stream cannot be opened
     */
    private String createWithOutputStream(String path, File file) throws IOException {
        try (OutputStream out = Files.newOutputStream(file.toPath(), StandardOpenOption.CREATE_NEW)) {
            return success(path, out.getClass().getSimpleName());
        }
    }

    /**
     * Creates the file using {@link Files#newBufferedWriter(Path, java.nio.charset.Charset, java.nio.file.OpenOption...)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file handle used for platform checks
     * @return success message describing the writer used
     * @throws IOException if the buffered writer cannot be opened
     */
    private String createWithBufferedWriter(String path, File file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW)) {
            return success(path, writer.getClass().getSimpleName());
        }
    }

    /**
     * Creates the file using {@link Files#newByteChannel(Path, java.nio.file.OpenOption...)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file handle used for platform checks
     * @return success message describing the channel used
     * @throws IOException if the byte channel cannot be opened
     */
    private String createWithSeekableByteChannel(String path, File file) throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(file.toPath(), StandardOpenOption.CREATE_NEW)) {
            return success(path, channel.getClass().getSimpleName());
        }
    }

    /**
     * Creates the file using {@link FileChannel#open(Path, java.nio.file.OpenOption...)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file handle used for platform checks
     * @return success message describing the channel used
     * @throws IOException if the file channel cannot be opened
     */
    private String createWithFileChannel(String path, File file) throws IOException {
        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE_NEW)) {
            return success(path, channel.getClass().getSimpleName());
        }
    }

    /**
     * Creates the file using {@link File#createNewFile()}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file handle used for platform checks
     * @return success message with the boolean result of {@code createNewFile}
     * @throws IOException if the call fails due to I/O errors
     */
    private String createWithFileCreateNew(String path, File file) throws IOException {
        return success(path, Boolean.toString(file.createNewFile()));
    }

    /**
     * Creates a temporary file inside the configured directory via {@link File#createTempFile(String, String, File)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file handle used for platform checks
     * @return success message containing the created temp file path
     * @throws IOException if the temporary file cannot be created
     */
    private String createWithFileCreateTemp(String path, File file) throws IOException {
        return success(path, File.createTempFile("", "", file).getAbsolutePath());
    }

    /**
     * Creates the file using {@link Files#createFile(Path, java.nio.file.attribute.FileAttribute...)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file handle used for platform checks
     * @return success message describing the created file
     * @throws IOException if the file already exists or cannot be created
     */
    private String createWithFilesCreateFile(String path, File file) throws IOException {
        return success(path, Files.createFile(file.toPath()).toString());
    }

    /**
     * Creates a temporary file relative to the provided parent path via {@link Files#createTempFile(Path, String, String, java.nio.file.attribute.FileAttribute[])}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file handle used for platform checks
     * @return success message containing the created temp path
     * @throws IOException if the temporary file cannot be created
     */
    private String createWithFilesCreateTempFile(String path, File file) throws IOException {
        return success(path, Files.createTempFile(file.toPath(), "", "").toString());
    }

    /**
     * Creates the file by writing an empty byte array via {@link Files#write(Path, byte[], java.nio.file.OpenOption...)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file handle used for platform checks
     * @return success message containing the resulting path
     * @throws IOException if the write fails
     */
    private String createWithFilesWriteBytes(String path, File file) throws IOException {
        return success(path, Files.write(file.toPath(), new byte[0], StandardOpenOption.CREATE_NEW).toString());
    }

    /**
     * Creates the file by writing an empty string via {@link Files#writeString(Path, CharSequence, java.nio.file.OpenOption...)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file handle used for platform checks
     * @return success message containing the resulting path
     * @throws IOException if the write fails
     */
    private String createWithFilesWriteString(String path, File file) throws IOException {
        return success(path, Files.writeString(file.toPath(), "", StandardOpenOption.CREATE_NEW).toString());
    }
}
