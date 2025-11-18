package de.tum.cit.aet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Demonstrates sending data through TCP and UDP abstractions.
 *
 * <p>Description: Establishes loopback servers and transmits payloads through
 * multiple APIs, returning normalized status messages for each success or
 * failure.
 *
 * <p>Design Rationale: Collecting send examples in one class keeps the audit
 * surface small and highlights the parity across blocking and non-blocking I/O.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
public class NetworkSystemSendAccess extends ProtectedRessourceAccess {

    /**
     * Number of send strategies implemented by this class.
     */
    private static final int AMOUNT_OF_METHODS = 6;

    /**
     * Payload transmitted to the sample servers.
     */
    private static final byte[] PAYLOAD = "network-payload".getBytes(StandardCharsets.UTF_8);

    /**
     * Reports the number of supported send methods.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return total send demonstrations
     */
    public static int getAmountOfMethods() {
        return AMOUNT_OF_METHODS;
    }

    /**
     * Lists the host and placeholder port describing the resources used.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return immutable list containing host and placeholder port
     */
    @Override
    public List<String> listHandeledRessources() {
        return List.of(LOOPBACK_HOST, "0");
    }

    /**
     * Creates localized success and failure messages for send attempts.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param parameters array containing the resource, payload, and optional id
     * @return success and failure message templates
     */
    @Override
    public List<String> getMessages(String[] parameters) {
        String resource = "";
        if (parameters.length > 0) {
            resource = parameters[0];
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
                String.format("Successfully sent data via %s%s", resource, suffix),
                String.format("Failed to send data via %s for operation id %s", resource, id));
    }

    /**
     * Executes the requested send method by id.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier of the send strategy to run
     * @return formatted status message describing the send result
     * @throws IOException if sending fails
     */
    @Override
    public String accessProtectedRessourceById(int id) throws IOException {
        String resource = listHandeledRessources().get(0);
        return switch (id) {
            case 1 -> sendWithSocketOutputStream(resource);
            case 2 -> sendWithBufferedWriter(resource);
            case 3 -> sendWithSocketChannel(resource);
            case 4 -> sendWithAsynchronousSocketChannel(resource);
            case 5 -> sendWithDatagramSocket(resource);
            case 6 -> sendWithDatagramChannel(resource);
            default -> failure(resource, id);
        };
    }

    /**
     * Sends data via {@link java.net.Socket#getOutputStream()}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual descriptor for the send channel
     * @return success message describing the destination port
     * @throws IOException if network operations fail
     */
    private String sendWithSocketOutputStream(String resource) throws IOException {
        try (LoopbackTcpAcceptServer server = new LoopbackTcpAcceptServer("send-tcp-server")) {
            writeWithSocketOutputStream(server.host(), server.port(), PAYLOAD);
            awaitLatch(server.latch(), "TCP server");
            return success(resource, describePort("Socket.getOutputStream", server.port()));
        }
    }

    /**
     * Sends data via {@link java.io.BufferedWriter#write(String)} on the socket output stream.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual descriptor for the send channel
     * @return success message describing the destination port
     * @throws IOException if network operations fail
     */
    private String sendWithBufferedWriter(String resource) throws IOException {
        try (LoopbackTcpAcceptServer server = new LoopbackTcpAcceptServer("send-tcp-server")) {
            writeWithBufferedWriter(server.host(), server.port(), "BufferedWriter payload");
            awaitLatch(server.latch(), "TCP server");
            return success(resource, describePort("BufferedWriter", server.port()));
        }
    }

    /**
     * Sends data via {@link java.nio.channels.SocketChannel#write(ByteBuffer)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual descriptor for the send channel
     * @return success message describing the destination port
     * @throws IOException if network operations fail
     */
    private String sendWithSocketChannel(String resource) throws IOException {
        try (LoopbackTcpAcceptServer server = new LoopbackTcpAcceptServer("send-tcp-server")) {
            writeWithSocketChannel(server.host(), server.port(), PAYLOAD);
            awaitLatch(server.latch(), "TCP server");
            return success(resource, describePort("SocketChannel.write", server.port()));
        }
    }

    /**
     * Sends data via {@link java.nio.channels.AsynchronousSocketChannel#write(ByteBuffer)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual descriptor for the send channel
     * @return success message describing the destination port
     * @throws IOException if network operations fail
     */
    private String sendWithAsynchronousSocketChannel(String resource) throws IOException {
        try (LoopbackTcpAcceptServer server = new LoopbackTcpAcceptServer("send-tcp-server")) {
            writeWithAsynchronousSocketChannel(server.host(), server.port(), PAYLOAD);
            awaitLatch(server.latch(), "TCP server");
            return success(resource, describePort("AsynchronousSocketChannel.write", server.port()));
        }
    }

    /**
     * Sends data via {@link java.net.DatagramSocket#send(java.net.DatagramPacket)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual descriptor for the send channel
     * @return success message describing the destination port
     * @throws IOException if network operations fail
     */
    private String sendWithDatagramSocket(String resource) throws IOException {
        try (LoopbackUdpSinkServer server = new LoopbackUdpSinkServer(PAYLOAD.length)) {
            writeWithDatagramSocket(server.host(), server.port(), PAYLOAD);
            awaitLatch(server.latch(), "UDP server");
            return success(resource, describePort("DatagramSocket.send", server.port()));
        }
    }

    /**
     * Sends data via {@link java.nio.channels.DatagramChannel#write(ByteBuffer)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual descriptor for the send channel
     * @return success message describing the destination port
     * @throws IOException if network operations fail
     */
    private String sendWithDatagramChannel(String resource) throws IOException {
        try (LoopbackUdpSinkServer server = new LoopbackUdpSinkServer(PAYLOAD.length)) {
            writeWithDatagramChannel(server.host(), server.port(), PAYLOAD);
            awaitLatch(server.latch(), "UDP server");
            return success(resource, describePort("DatagramChannel.send", server.port()));
        }
    }
}
