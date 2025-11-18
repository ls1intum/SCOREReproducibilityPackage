package de.tum.cit.aet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Demonstrates receiving data through TCP and UDP abstractions.
 *
 * <p>Description: Starts loopback servers, retrieves payloads through multiple
 * APIs, and returns normalized status messages describing the result.
 *
 * <p>Design Rationale: Consolidates network receive logic to make auditing and
 * extending the sample easier while ensuring consistent messaging.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
public class NetworkSystemReceiveAccess extends ProtectedRessourceAccess {

    /**
     * Number of receive strategies implemented by this class.
     */
    private static final int AMOUNT_OF_METHODS = 6;

    /**
     * Payload used by the loopback TCP server.
     */
    private static final byte[] TCP_PAYLOAD = "loopback-response".getBytes(StandardCharsets.UTF_8);

    /**
     * Payload used by the loopback UDP server.
     */
    private static final byte[] UDP_PAYLOAD = "udp-response".getBytes(StandardCharsets.UTF_8);

    /**
     * Reports the supported receive methods.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return total receive demonstrations
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
     * Creates localized success and failure messages for receive attempts.
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
                String.format("Successfully received data via %s%s", resource, suffix),
                String.format("Failed to receive data via %s for operation id %s", resource, id));
    }

    /**
     * Executes the requested receive method by id.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier of the receive strategy to run
     * @return formatted status message describing the read result
     * @throws IOException if reading fails
     */
    @Override
    public String accessProtectedRessourceById(int id) throws IOException {
        String resource = listHandeledRessources().get(0);
        return switch (id) {
            case 1 -> receiveWithSocketInputStream(resource);
            case 2 -> receiveWithBufferedReader(resource);
            case 3 -> receiveWithSocketChannel(resource);
            case 4 -> receiveWithAsynchronousSocketChannel(resource);
            case 5 -> receiveWithDatagramSocket(resource);
            case 6 -> receiveWithDatagramChannel(resource);
            default -> failure(resource, id);
        };
    }

    /**
     * Receives data via {@link java.net.Socket#getInputStream()}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual descriptor for the receive channel
     * @return success message describing the result
     * @throws IOException if network operations fail
     */
    private String receiveWithSocketInputStream(String resource) throws IOException {
        try (LoopbackTcpPayloadServer server = new LoopbackTcpPayloadServer(TCP_PAYLOAD)) {
            String result = readFromSocketInputStream(server.host(), server.port());
            awaitLatch(server.latch(), "TCP response");
            return success(resource, describeResult("Socket.getInputStream", result));
        }
    }

    /**
     * Receives data via {@link java.io.BufferedReader#readLine()} on the socket input stream.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual descriptor for the receive channel
     * @return success message describing the result
     * @throws IOException if network operations fail
     */
    private String receiveWithBufferedReader(String resource) throws IOException {
        try (LoopbackTcpPayloadServer server = new LoopbackTcpPayloadServer(TCP_PAYLOAD)) {
            String result = readWithBufferedReader(server.host(), server.port());
            awaitLatch(server.latch(), "TCP response");
            return success(resource, describeResult("BufferedReader", result));
        }
    }

    /**
     * Receives data via {@link java.nio.channels.SocketChannel#read(ByteBuffer)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual descriptor for the receive channel
     * @return success message describing the result
     * @throws IOException if network operations fail
     */
    private String receiveWithSocketChannel(String resource) throws IOException {
        try (LoopbackTcpPayloadServer server = new LoopbackTcpPayloadServer(TCP_PAYLOAD)) {
            String result = readWithSocketChannel(server.host(), server.port(), TCP_PAYLOAD.length);
            awaitLatch(server.latch(), "TCP response");
            return success(resource, describeResult("SocketChannel.read", result));
        }
    }

    /**
     * Receives data via {@link java.nio.channels.AsynchronousSocketChannel#read(ByteBuffer)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual descriptor for the receive channel
     * @return success message describing the result
     * @throws IOException if network operations fail
     */
    private String receiveWithAsynchronousSocketChannel(String resource) throws IOException {
        try (LoopbackTcpPayloadServer server = new LoopbackTcpPayloadServer(TCP_PAYLOAD)) {
            String result = readWithAsynchronousSocketChannel(server.host(), server.port(), TCP_PAYLOAD.length);
            awaitLatch(server.latch(), "TCP response");
            return success(resource, describeResult("AsynchronousSocketChannel.read", result));
        }
    }

    /**
     * Receives data via {@link java.net.DatagramSocket#receive(java.net.DatagramPacket)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual descriptor for the receive channel
     * @return success message describing the result
     * @throws IOException if network operations fail
     */
    private String receiveWithDatagramSocket(String resource) throws IOException {
        try (LoopbackUdpPayloadServer server = new LoopbackUdpPayloadServer(UDP_PAYLOAD)) {
            String result = readWithDatagramSocket(server.host(), server.port(), UDP_PAYLOAD.length);
            awaitLatch(server.latch(), "UDP response");
            return success(resource, describeResult("DatagramSocket.receive", result));
        }
    }

    /**
     * Receives data via {@link java.nio.channels.DatagramChannel#read(ByteBuffer)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual descriptor for the receive channel
     * @return success message describing the result
     * @throws IOException if network operations fail
     */
    private String receiveWithDatagramChannel(String resource) throws IOException {
        try (LoopbackUdpPayloadServer server = new LoopbackUdpPayloadServer(UDP_PAYLOAD)) {
            String result = readWithDatagramChannel(server.host(), server.port(), UDP_PAYLOAD.length);
            awaitLatch(server.latch(), "UDP response");
            return success(resource, describeResult("DatagramChannel.receive", result));
        }
    }
}
