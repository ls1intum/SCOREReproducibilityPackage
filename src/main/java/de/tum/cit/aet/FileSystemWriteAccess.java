package de.tum.cit.aet;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Provides demonstrations for writing files via multiple APIs.
 *
 * <p>Description: Covers writer, stream, and channel-based approaches to file
 * modification so policy enforcement can be exercised regardless of which API
 * is used.
 *
 * <p>Design Rationale: Grouping the implementations together improves
 * maintainability and ensures each write method uses the same messaging
 * contract.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
public class FileSystemWriteAccess extends ProtectedRessourceAccess {

    /**
     * Number of write strategies showcased by this class.
     */
    private static final int AMOUNT_OF_METHODS = 12;

    /**
     * Reports the number of supported write methods.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return total amount of write demonstrations
     */
    public static int getAmountOfMethods() {
        return AMOUNT_OF_METHODS;
    }

    /**
     * Lists the file paths targeted by the write demonstrations.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return immutable list containing the resource to modify
     */
    @Override
    public List<String> listHandeledRessources() {
        return List.of("resources/FileToWrite.txt");
    }

    /**
     * Creates localized success and failure messages for write attempts.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param parameters array containing the path, payload, and optional id
     * @return success and failure message strings
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
                String.format("Successfully written resource at %s%s", path, suffix),
                String.format("Failed to write resource at %s for operation id %s", path, id));
    }

    /**
     * Executes the write method identified by id.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier of the write strategy to run
     * @return textual description of the write result
     * @throws IOException if writing fails
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
            case 1 -> writeWithFileWriter(pathAsString, file);
            case 2 -> writeWithFileOutputStream(pathAsString, file);
            case 3 -> writeWithBufferedWriter(pathAsString, file);
            case 4 -> writeWithBufferedOutputStream(pathAsString, file);
            case 5 -> writeWithDataOutputStream(pathAsString, file);
            case 6 -> writeWithObjectOutputStream(pathAsString, file);
            case 7 -> writeWithPrintWriter(pathAsString, file);
            case 8 -> writeWithFilesWriteString(pathAsString, file);
            case 9 -> writeWithFilesWrite(pathAsString, file);
            case 10 -> writeWithNewOutputStream(pathAsString, file);
            case 11 -> writeWithFileChannel(pathAsString, file);
            case 12 -> writeWithSeekableByteChannel(pathAsString, file);
            default -> failure(pathAsString, id);
        };
    }

    /**
     * Writes sample content via {@link FileWriter#FileWriter(File, boolean)} and {@link FileWriter#write(String)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle being modified
     * @return success message confirming the write
     * @throws IOException if writing fails
     */
    private String writeWithFileWriter(String path, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write("Hello from FileWriter");
        }
        return success(path, "");
    }

    /**
     * Writes sample content via {@link FileOutputStream#FileOutputStream(File, boolean)} and {@link FileOutputStream#write(byte[])}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle being modified
     * @return success message confirming the write
     * @throws IOException if writing fails
     */
    private String writeWithFileOutputStream(String path, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            fos.write("Hello from FileOutputStream".getBytes(StandardCharsets.UTF_8));
        }
        return success(path, "");
    }

    /**
     * Writes sample content via {@link BufferedWriter#BufferedWriter(java.io.Writer)} and {@link BufferedWriter#write(String)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle being modified
     * @return success message confirming the write
     * @throws IOException if writing fails
     */
    private String writeWithBufferedWriter(String path, File file) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
            bw.write("Hello from BufferedWriter");
        }
        return success(path, "");
    }

    /**
     * Writes sample content via {@link BufferedOutputStream#BufferedOutputStream(OutputStream)} and {@link BufferedOutputStream#write(byte[])}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle being modified
     * @return success message confirming the write
     * @throws IOException if writing fails
     */
    private String writeWithBufferedOutputStream(String path, File file) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file, false))) {
            bos.write("Hello from BufferedOutputStream".getBytes(StandardCharsets.UTF_8));
        }
        return success(path, "");
    }

    /**
     * Writes sample content via {@link DataOutputStream#DataOutputStream(OutputStream)} and {@link DataOutputStream#writeUTF(String)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle being modified
     * @return success message confirming the write
     * @throws IOException if writing fails
     */
    private String writeWithDataOutputStream(String path, File file) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file, false))) {
            dos.writeUTF("Hello from DataOutputStream.writeUTF");
        }
        return success(path, "");
    }

    /**
     * Writes sample content via {@link ObjectOutputStream#ObjectOutputStream(OutputStream)} and {@link ObjectOutputStream#writeObject(Object)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle being modified
     * @return success message confirming the write
     * @throws IOException if writing fails
     */
    private String writeWithObjectOutputStream(String path, File file) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file, false))) {
            oos.writeObject("Hello from ObjectOutputStream");
        }
        return success(path, "");
    }

    /**
     * Writes sample content via {@link PrintWriter#PrintWriter(File)} and {@link PrintWriter#println(String)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle being modified
     * @return success message confirming the write
     * @throws IOException if writing fails
     */
    private String writeWithPrintWriter(String path, File file) throws IOException {
        try (PrintWriter pw = new PrintWriter(file)) {
            pw.println("Hello from PrintWriter");
        }
        return success(path, "");
    }

    /**
     * Writes sample content using {@link Files#newOutputStream(Path, java.nio.file.OpenOption...)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle being modified
     * @return success message describing the stream used
     * @throws IOException if writing fails
     */
    private String writeWithNewOutputStream(String path, File file) throws IOException {
        try (OutputStream out = Files.newOutputStream(file.toPath(), StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            out.write("Hello from Files.newOutputStream".getBytes(StandardCharsets.UTF_8));
            return success(path, out.getClass().getSimpleName());
        }
    }

    /**
     * Writes sample content using {@link FileChannel#open(Path, java.nio.file.OpenOption...)} and {@link FileChannel#write(ByteBuffer)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle being modified
     * @return success message describing the channel used
     * @throws IOException if writing fails
     */
    private String writeWithFileChannel(String path, File file) throws IOException {
        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap("Hello from FileChannel".getBytes(StandardCharsets.UTF_8)));
            return success(path, channel.getClass().getSimpleName());
        }
    }

    /**
     * Writes sample content using {@link Files#newByteChannel(Path, java.nio.file.OpenOption...)} and {@link SeekableByteChannel#write(ByteBuffer)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle being modified
     * @return success message describing the channel used
     * @throws IOException if writing fails
     */
    private String writeWithSeekableByteChannel(String path, File file) throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(file.toPath(), StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap("Hello from SeekableByteChannel".getBytes(StandardCharsets.UTF_8)));
            return success(path, channel.getClass().getSimpleName());
        }
    }

    /**
     * Writes sample content using {@link Files#writeString(Path, CharSequence, java.nio.charset.Charset, java.nio.file.OpenOption...)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle being modified
     * @return success message containing the resulting path
     * @throws IOException if writing fails
     */
    private String writeWithFilesWriteString(String path, File file) throws IOException {
        return success(path,
                Files.writeString(file.toPath(), "Hello from Files.writeString", StandardCharsets.UTF_8).toString());
    }

    /**
     * Writes sample content using {@link Files#write(Path, Iterable, java.nio.charset.Charset, java.nio.file.OpenOption...)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle being modified
     * @return success message containing the resulting path
     * @throws IOException if writing fails
     */
    private String writeWithFilesWrite(String path, File file) throws IOException {
        return success(path,
                Files.write(file.toPath(), List.of("Hello from Files.write"), StandardCharsets.UTF_8).toString());
    }
}
