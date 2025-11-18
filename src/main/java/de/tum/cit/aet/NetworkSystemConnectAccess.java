package de.tum.cit.aet;

import java.io.IOException;
import java.util.List;

/**
 * Demonstrates different ways to establish outbound network connections.
 *
 * <p>Description: Covers socket constructors, factories, and NIO channels so
 * blocked/allowed behaviors remain consistent regardless of the API layer.
 *
 * <p>Design Rationale: Centralizes the connection approaches to simplify
 * auditing and provide a unified messaging contract.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
public class NetworkSystemConnectAccess extends ProtectedRessourceAccess {

    /**
     * Number of connection strategies implemented by this class.
     */
    private static final int AMOUNT_OF_METHODS = 6;

    /**
     * Reports the amount of supported connection methods.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return total connection demonstrations
     */
    public static int getAmountOfMethods() {
        return AMOUNT_OF_METHODS;
    }

    /**
     * Lists the loopback host and placeholder port used for demonstrations.
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
     * Builds localized success and failure messages for connection attempts.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param parameters array containing the host, payload, and optional id
     * @return list containing success and failure message templates
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
                String.format("Successfully connected to %s%s", resource, suffix),
                String.format("Failed to connect to %s for operation id %s", resource, id));
    }

    /**
     * Executes the requested connection method by id.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier of the connection strategy to use
     * @return formatted description of the connection result
     * @throws IOException if the connection attempt fails
     */
    @Override
    public String accessProtectedRessourceById(int id) throws IOException {
        String resource = listHandeledRessources().get(0);
        return switch (id) {
            case 1 -> connectWithSocketConstructor(resource);
            case 2 -> connectWithInetAddress(resource);
            case 3 -> connectWithSocketConnect(resource);
            case 4 -> connectWithSocketFactory(resource);
            case 5 -> connectWithSocketChannel(resource);
            case 6 -> connectWithAsynchronousSocketChannel(resource);
            default -> failure(resource, id);
        };
    }

    /**
     * Connects using {@link java.net.Socket#Socket(String, int)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the resource being accessed
     * @return success message describing the connected port
     * @throws IOException if the socket connection fails
     */
    private String connectWithSocketConstructor(String resource) throws IOException {
        try (LoopbackTcpAcceptServer server = new LoopbackTcpAcceptServer("connect-server")) {
            openSocket(server.host(), server.port());
            awaitLatch(server.latch(), "loopback connect server");
            return success(resource, describePort("Socket(host,port)", server.port()));
        }
    }

    /**
     * Connects using {@link java.net.Socket#Socket(java.net.InetAddress, int)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the resource being accessed
     * @return success message describing the connected port
     * @throws IOException if the socket connection fails
     */
    private String connectWithInetAddress(String resource) throws IOException {
        try (LoopbackTcpAcceptServer server = new LoopbackTcpAcceptServer("connect-server")) {
            openSocket(LOOPBACK, server.port());
            awaitLatch(server.latch(), "loopback connect server");
            return success(resource, describePort("Socket(InetAddress,port)", server.port()));
        }
    }

    /**
     * Connects using {@link java.net.Socket#connect(java.net.SocketAddress)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the resource being accessed
     * @return success message describing the connected port
     * @throws IOException if the socket connection fails
     */
    private String connectWithSocketConnect(String resource) throws IOException {
        try (LoopbackTcpAcceptServer server = new LoopbackTcpAcceptServer("connect-server")) {
            connectWithExplicitSocket(server.host(), server.port());
            awaitLatch(server.latch(), "loopback connect server");
            return success(resource, describePort("Socket.connect", server.port()));
        }
    }

    /**
     * Connects using {@link javax.net.SocketFactory#createSocket(String, int)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the resource being accessed
     * @return success message describing the connected port
     * @throws IOException if the socket connection fails
     */
    private String connectWithSocketFactory(String resource) throws IOException {
        try (LoopbackTcpAcceptServer server = new LoopbackTcpAcceptServer("connect-server")) {
            openSocketFromFactory(server.host(), server.port());
            awaitLatch(server.latch(), "loopback connect server");
            return success(resource, describePort("SocketFactory.createSocket", server.port()));
        }
    }

    /**
     * Connects using {@link java.nio.channels.SocketChannel#open(java.net.SocketAddress)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the resource being accessed
     * @return success message describing the connected port
     * @throws IOException if the socket connection fails
     */
    private String connectWithSocketChannel(String resource) throws IOException {
        try (LoopbackTcpAcceptServer server = new LoopbackTcpAcceptServer("connect-server")) {
            openSocketChannel(server.host(), server.port());
            awaitLatch(server.latch(), "loopback connect server");
            return success(resource, describePort("SocketChannel.open", server.port()));
        }
    }

    /**
     * Connects using {@link java.nio.channels.AsynchronousSocketChannel#connect(java.net.SocketAddress)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the resource being accessed
     * @return success message describing the connected port
     * @throws IOException if the socket connection fails
     */
    private String connectWithAsynchronousSocketChannel(String resource) throws IOException {
        try (LoopbackTcpAcceptServer server = new LoopbackTcpAcceptServer("connect-server")) {
            openAsyncSocketChannel(server.host(), server.port());
            awaitLatch(server.latch(), "loopback connect server");
            return success(resource, describePort("AsynchronousSocketChannel.connect", server.port()));
        }
    }
}
