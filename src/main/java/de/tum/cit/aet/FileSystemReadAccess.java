package de.tum.cit.aet;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Demonstrates reading files through multiple I/O primitives.
 *
 * <p>Description: Collects the most common read patterns so security controls
 * can audit file access consistently regardless of which API a developer
 * chooses.
 *
 * <p>Design Rationale: Centralizing the read demonstrations improves
 * maintainability and highlights parity between legacy and modern file APIs.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
public class FileSystemReadAccess extends ProtectedRessourceAccess {

    /**
     * Number of read techniques exposed by this access class.
     */
    private static final int AMOUNT_OF_METHODS = 12;

    /**
     * Reports the number of supported read methods.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return total amount of read demonstrations
     */
    public static int getAmountOfMethods() {
        return AMOUNT_OF_METHODS;
    }

    /**
     * Lists the file paths that each read method targets.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return immutable list containing the path to read
     */
    @Override
    public List<String> listHandeledRessources() {
        return List.of("resources/FileToRead.txt");
    }

    /**
     * Builds localized success and failure messages for read attempts.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param parameters array containing the path, payload, and optional id
     * @return list with the success message followed by the failure message
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
            suffix = String.format(" with result: %s", payload);
        }
        return List.of(
                String.format("Successfully read resource at %s%s", path, suffix),
                String.format("Failed to read resource at %s for operation id %s", path, id));
    }

    /**
     * Executes the requested read method by id.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier of the read strategy to execute
     * @return textual description of the read result
     * @throws IOException if the read fails
     */
    @Override
    public String accessProtectedRessourceById(int id) throws IOException {
        String pathAsString = listHandeledRessources().get(0);
        Path path = Path.of(pathAsString);
        File file = path.toFile();
        if (!Files.exists(path) || !file.canRead()) {
            return failure(pathAsString, id);
        }
        return switch (id) {
            case 1 -> readWithFileReader(pathAsString, file);
            case 2 -> readWithFileInputStream(pathAsString, file);
            case 3 -> readWithBufferedReader(pathAsString, file);
            case 4 -> readWithBufferedInputStream(pathAsString, file);
            case 5 -> readWithDataInputStream(pathAsString, file);
            case 6 -> readWithObjectInputStream(pathAsString, file);
            case 7 -> readWithScanner(pathAsString, file);
            case 8 -> readWithFilesReadString(pathAsString, file);
            case 9 -> readWithFilesReadAllLines(pathAsString, file);
            case 10 -> readWithNewInputStream(pathAsString, file);
            case 11 -> readWithFileChannel(pathAsString, file);
            case 12 -> readWithSeekableByteChannel(pathAsString, file);
            default -> failure(pathAsString, id);
        };
    }

    /**
     * Reads the file via {@link FileReader#FileReader(File)} and {@link java.io.Reader#transferTo(java.io.Writer)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle pointing to the resource
     * @return success message containing the read payload
     * @throws IOException if reading fails
     */
    private String readWithFileReader(String path, File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            StringWriter sw = new StringWriter();
            reader.transferTo(sw);
            return success(path, sw.toString());
        }
    }

    /**
     * Reads the file via {@link FileInputStream#FileInputStream(File)} and {@link FileInputStream#readAllBytes()}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle pointing to the resource
     * @return success message containing the read payload
     * @throws IOException if reading fails
     */
    private String readWithFileInputStream(String path, File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return success(path, new String(fis.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Reads the file via {@link BufferedReader#BufferedReader(java.io.Reader)} and {@link BufferedReader#lines()}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle pointing to the resource
     * @return success message containing the read payload
     * @throws IOException if reading fails
     */
    private String readWithBufferedReader(String path, File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            return success(path, br.lines().collect(Collectors.joining(System.lineSeparator())));
        }
    }

    /**
     * Reads the file via {@link BufferedInputStream#BufferedInputStream(InputStream)} and {@link BufferedInputStream#readAllBytes()}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle pointing to the resource
     * @return success message containing the read payload
     * @throws IOException if reading fails
     */
    private String readWithBufferedInputStream(String path, File file) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            return success(path, new String(bis.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Reads the file via {@link DataInputStream#DataInputStream(InputStream)} and {@link DataInputStream#readUTF()}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle pointing to the resource
     * @return success message containing the read payload or a descriptive error
     * @throws IOException if reading fails
     */
    private String readWithDataInputStream(String path, File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            return success(path, dis.readUTF());
        } catch (EOFException e) {
            return success(path, "Hello from expected EOFException");
        }
    }

    /**
     * Reads the file via {@link ObjectInputStream#ObjectInputStream(InputStream)} and {@link ObjectInputStream#readObject()}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle pointing to the resource
     * @return success message containing the read payload or diagnostics
     * @throws IOException if reading fails
     */
    private String readWithObjectInputStream(String path, File file) throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return success(path, String.valueOf(ois.readObject()));
        } catch (ClassNotFoundException e) {
            return success(path, "Hello from expected ClassNotFoundException");
        } catch (StreamCorruptedException e) {
            return success(path, "Hello from expected StreamCorruptedException");
        }
    }

    /**
     * Reads the file via {@link Scanner#hasNextLine()} followed by {@link Scanner#nextLine()}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle pointing to the resource
     * @return success message containing the first line read (if any)
     * @throws IOException if reading fails
     */
    private String readWithScanner(String path, File file) throws IOException {
        try (Scanner sc = new Scanner(file, StandardCharsets.UTF_8)) {
            String result = "";
            if (sc.hasNextLine()) {
                result = sc.nextLine();
            }
            return success(path, result);
        }
    }

    /**
     * Reads the file using {@link Files#readString(Path, java.nio.charset.Charset)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle pointing to the resource
     * @return success message containing the read payload
     * @throws IOException if reading fails
     */
    private String readWithFilesReadString(String path, File file) throws IOException {
        return success(path, Files.readString(file.toPath(), StandardCharsets.UTF_8));
    }

    /**
     * Reads the file using {@link Files#readAllLines(Path, java.nio.charset.Charset)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle pointing to the resource
     * @return success message containing the read payload
     * @throws IOException if reading fails
     */
    private String readWithFilesReadAllLines(String path, File file) throws IOException {
        return success(path,
                String.join(System.lineSeparator(), Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)));
    }

    /**
     * Reads the file using {@link Files#newInputStream(Path, java.nio.file.OpenOption...)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle pointing to the resource
     * @return success message containing the read payload
     * @throws IOException if reading fails
     */
    private String readWithNewInputStream(String path, File file) throws IOException {
        try (InputStream in = Files.newInputStream(file.toPath(), StandardOpenOption.READ)) {
            return success(path, new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Reads the file using {@link FileChannel#open(Path, java.nio.file.OpenOption...)} and {@link FileChannel#read(ByteBuffer)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle pointing to the resource
     * @return success message containing the read payload
     * @throws IOException if reading fails
     */
    private String readWithFileChannel(String path, File file) throws IOException {
        try (FileChannel ch = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.allocate((int) ch.size());
            ch.read(buf);
            return success(path, new String(buf.array(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Reads the file using {@link Files#newByteChannel(Path, java.nio.file.OpenOption...)} and {@link SeekableByteChannel#read(ByteBuffer)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path textual representation of the file path
     * @param file file handle pointing to the resource
     * @return success message containing the read payload
     * @throws IOException if reading fails
     */
    private String readWithSeekableByteChannel(String path, File file) throws IOException {
        try (SeekableByteChannel ch = Files.newByteChannel(file.toPath(), StandardOpenOption.READ)) {
            ByteBuffer buf = ByteBuffer.allocate((int) ch.size());
            ch.read(buf);
            return success(path, new String(buf.array(), StandardCharsets.UTF_8));
        }
    }
}
