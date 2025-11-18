package de.tum.cit.aet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.SocketFactory;

/**
 * Base class for all resource access demonstrations.
 *
 * <p>Description: Provides shared utilities for executing commands, accessing
 * files, sending network traffic, and coordinating threads so subclasses can
 * focus on exposing the high-level operations.
 *
 * <p>Design Rationale: Centralizes reusable helpers to ensure consistent error
 * handling, messaging, and loopback infrastructure across access types.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
public abstract class ProtectedRessourceAccess {

    /**
     * Loopback address used for local network demonstrations.
     */
    protected static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();

    /**
     * String representation of the loopback host used in log messages.
     */
    protected static final String LOOPBACK_HOST = LOOPBACK.getHostAddress();

    /**
     * Lists the logical resources handled by the concrete access implementation.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return immutable list describing the resources
     */
    public abstract List<String> listHandeledRessources();

    /**
     * Provides success and failure message templates for the implementation.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param parameters context array containing path/payload details
     * @return list containing the success and failure message text
     */
    public abstract List<String> getMessages(String[] parameters);

    /**
     * Executes the protected resource access flow identified by id.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier of the operation to run
     * @return formatted outcome description
     * @throws IOException if the operation fails
     * @throws InterruptedException if the operation is interrupted
     */
    public abstract String accessProtectedRessourceById(int id) throws IOException, InterruptedException;

    /**
     * Formats the success message using the configured templates.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path logical path or resource identifier involved in the call
     * @param payload optional payload describing the result
     * @return formatted success message
     */
    protected String success(String path, String payload) {
        return getMessages(new String[] { path, payload }).get(0);
    }

    /**
     * Formats the failure message using the configured templates.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param path logical path or resource identifier involved in the call
     * @param id identifier of the operation that failed
     * @return formatted failure message
     */
    protected String failure(String path, int id) {
        return getMessages(new String[] { path, "", String.valueOf(id) }).get(1);
    }

    /**
     * Validates whether the provided id falls within the supported range.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier that should be verified
     * @param amountOfMethods numbers of supported methods for the subclass
     * @return {@code true} when the id is within {@code [1, amountOfMethods]}
     */
    protected boolean isSupportedMethodId(int id, int amountOfMethods) {
        return id >= 1 && id <= amountOfMethods;
    }

    /**
     * Captures stdout, stderr, and exit code for a child process.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param prefix textual label describing the execution method
     * @param process running process whose result should be captured
     * @return formatted string that includes exit code and available output
     * @throws IOException if the current thread is interrupted while waiting
     */
    protected String captureProcessResult(String prefix, Process process) throws IOException {
        process.getOutputStream().close();
        try (InputStream stdout = process.getInputStream(); InputStream stderr = process.getErrorStream()) {
            int exitCode = process.waitFor();
            String out = new String(stdout.readAllBytes(), StandardCharsets.UTF_8).trim();
            String err = new String(stderr.readAllBytes(), StandardCharsets.UTF_8).trim();
            StringBuilder builder = new StringBuilder(prefix).append(" exit=").append(exitCode);
            if (!out.isEmpty()) {
                builder.append(" stdout=").append(out);
            }
            if (!err.isEmpty()) {
                builder.append(" stderr=").append(err);
            }
            return builder.toString();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(String.format("Interrupted while executing %s", prefix), e);
        }
    }

    /**
     * Captures the result of a multi-process pipeline and waits for all stages.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param prefix textual label describing the execution method
     * @param processes pipeline of processes created for the execution
     * @return formatted payload from the last stage
     * @throws IOException if any process fails or the wait is interrupted
     */
    protected String capturePipelineResult(String prefix, List<Process> processes) throws IOException {
        Process lastStage = processes.get(processes.size() - 1);
        String payload = captureProcessResult(prefix, lastStage);
        for (int i = 0; i < processes.size() - 1; i++) {
            try {
                processes.get(i).waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(String.format("Interrupted while waiting for pipeline stage %d", i + 1), e);
            }
        }
        return payload;
    }

    /**
     * Instantiates the first configured thread resource.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return Thread instance created from the configured resource
     * @throws IOException if instantiation fails or no resources are available
     */
    protected Thread instantiateThreads() throws IOException {
        List<String> resources = listHandeledRessources();
        if (resources.isEmpty()) {
            throw new IOException("No thread classes configured");
        }
        return instantiateThread(resources.get(0));
    }

    /**
     * Instantiates the provided class as either a Thread or Runnable.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param className fully qualified class name to instantiate
     * @return Thread ready for execution
     * @throws IOException if instantiation fails or the class is incompatible
     */
    protected Thread instantiateThread(String className) throws IOException {
        try {
            Class<?> clazz = Class.forName(className);
            Thread thread;
            if (Thread.class.isAssignableFrom(clazz)) {
                thread = (Thread) clazz.getDeclaredConstructor().newInstance();
            } else if (Runnable.class.isAssignableFrom(clazz)) {
                Runnable runnable = (Runnable) clazz.getDeclaredConstructor().newInstance();
                thread = new Thread(runnable);
            } else {
                throw new IOException(String.format("Resource %s is not a Thread or Runnable", className));
            }
            thread.setName(clazz.getSimpleName());
            return thread;
        } catch (ReflectiveOperationException e) {
            throw new IOException(String.format("Failed to instantiate %s", className), e);
        }
    }

    /**
     * Starts each thread sequentially while applying the provided context prefix.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param threads threads to start
     * @param contextPrefix prefix describing the operation for logging
     * @throws IOException if any thread fails to run
     */
    protected void startThreadsSequentially(List<Thread> threads, String contextPrefix) throws IOException {
        for (Thread thread : threads) {
            startThreadAndAwait(thread, contextPrefix + thread.getName());
        }
    }

    /**
     * Starts a thread and waits for its completion using a default context.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param thread thread to start
     * @throws IOException if the thread fails to run
     */
    protected void startThreadAndAwait(Thread thread) throws IOException {
        startThreadAndAwait(thread, "starting configured threads");
    }

    /**
     * Starts the provided thread and waits for it to finish.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param thread thread to start
     * @param context textual description used when reporting interruptions
     * @throws IOException if the thread is interrupted while running
     */
    protected void startThreadAndAwait(Thread thread, String context) throws IOException {
        try {
            thread.start();
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while " + context, e);
        }
    }

    /**
     * Waits for the provided latch and wraps interruptions in IOException.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param latch latch to await
     * @param context textual description of the awaited operation
     * @throws IOException if interrupted while waiting
     */
    protected void awaitLatch(CountDownLatch latch, String context) throws IOException {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for " + context, e);
        }
    }

    /**
     * Waits for a collection of futures to complete and propagates errors.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param futures futures to await
     * @param context textual description of the awaited operation
     * @throws IOException if waiting is interrupted or the task fails
     */
    protected void awaitFutures(List<? extends Future<?>> futures, String context) throws IOException {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for " + context, e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof IOException io) {
                    throw io;
                }
                throw new IOException(context + " failed", cause);
            }
        }
    }

    /**
     * Waits for the provided fork-join tasks to complete.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param tasks fork-join tasks to join
     * @param context textual description of the awaited operation
     * @throws IOException if any task throws an IOException
     */
    protected void joinForkJoinTasks(List<ForkJoinTask<Void>> tasks, String context) throws IOException {
        for (ForkJoinTask<Void> task : tasks) {
            try {
                task.join();
            } catch (RuntimeException e) {
                Throwable cause = e.getCause();
                if (cause instanceof IOException io) {
                    throw io;
                }
                throw new IOException(context + " failed", e);
            }
        }
    }

    /**
     * Waits for the provided CompletableFuture instances to complete.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param futures futures to join
     * @param context textual description of the awaited operation
     * @throws IOException if a future completes exceptionally
     */
    protected void joinCompletableFutures(List<CompletableFuture<Void>> futures, String context) throws IOException {
        CompletableFuture<Void> combined = CompletableFuture
                .allOf(futures.toArray(CompletableFuture[]::new));
        try {
            combined.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            throw new IOException(context + " failed", cause);
        }
    }

    /**
     * Joins a single CompletableFuture and wraps failures.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param future future to join
     * @param context textual description of the awaited operation
     * @param <T> type produced by the future
     * @return result produced by the future
     * @throws IOException if the future completes exceptionally
     */
    protected <T> T joinCompletableFuture(CompletableFuture<T> future, String context) throws IOException {
        try {
            return future.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            throw new IOException(context + " failed", cause);
        }
    }

    /**
     * Runs a parallel stream of threads and propagates I/O failures.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param stream stream producing threads to execute
     * @param context textual description of the awaited operation
     * @throws IOException if any thread fails to run
     */
    protected void runParallelThreadStream(Stream<Thread> stream, String context) throws IOException {
        try {
            stream.forEach(thread -> {
                try {
                    startThreadAndAwait(thread, context + " for " + thread.getName());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }

    /**
     * Shuts down the provided executor service and waits for termination.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param executor executor to terminate
     * @param context textual description used when reporting interruptions
     * @throws IOException if termination is interrupted
     */
    protected void shutdownExecutor(ExecutorService executor, String context) throws IOException {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while shutting down executor for " + context, e);
        }
    }

    /**
     * Builds a comma-separated description of the provided threads.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param threads threads being described
     * @return comma-separated list of thread names
     */
    protected String describeThreads(List<Thread> threads) {
        return threads.stream()
                .map(Thread::getName)
                .collect(Collectors.joining(", "));
    }

    /**
     * Executes the provided threads using {@link ExecutorService#execute(Runnable)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param threads threads to run
     * @return textual description of the execution path
     * @throws IOException if execution fails
     */
    protected String runExecutorExecute(List<Thread> threads) throws IOException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch latch = new CountDownLatch(threads.size());
        AtomicReference<IOException> failure = new AtomicReference<>();
        try {
            for (Thread thread : threads) {
                executor.execute(() -> {
                    try {
                        startThreadAndAwait(thread, "Executor.execute for " + thread.getName());
                    } catch (IOException e) {
                        failure.compareAndSet(null, e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            awaitLatch(latch, "Executor.execute");
            if (failure.get() != null) {
                throw failure.get();
            }
        } finally {
            shutdownExecutor(executor, "Executor.execute");
        }
        return "Executor.execute via " + describeThreads(threads);
    }

    /**
     * Executes the provided threads using {@link ExecutorService#submit(Runnable)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param threads threads to run
     * @return textual description of the execution path
     * @throws IOException if execution fails
     */
    protected String runExecutorServiceSubmit(List<Thread> threads) throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, threads.size()));
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (Thread thread : threads) {
                futures.add(executor.submit(() -> {
                    startThreadAndAwait(thread, "ExecutorService.submit for " + thread.getName());
                    return null;
                }));
            }
            awaitFutures(futures, "ExecutorService.submit");
        } finally {
            shutdownExecutor(executor, "ExecutorService.submit");
        }
        return "ExecutorService.submit via " + describeThreads(threads);
    }

    /**
     * Executes the provided threads using {@link ThreadPoolExecutor#submit(Runnable)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param threads threads to run
     * @return textual description of the execution path
     * @throws IOException if execution fails
     */
    protected String runThreadPoolExecutorSubmit(List<Thread> threads) throws IOException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                Math.max(1, threads.size()),
                Math.max(1, threads.size()),
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (Thread thread : threads) {
                futures.add(executor.submit(() -> {
                    startThreadAndAwait(thread, "ThreadPoolExecutor.submit for " + thread.getName());
                    return null;
                }));
            }
            awaitFutures(futures, "ThreadPoolExecutor.submit");
        } finally {
            shutdownExecutor(executor, "ThreadPoolExecutor.submit");
        }
        return "ThreadPoolExecutor.submit via " + describeThreads(threads);
    }

    /**
     * Executes the provided threads using {@link ScheduledExecutorService#schedule(Runnable, long, TimeUnit)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param threads threads to run
     * @return textual description of the execution path
     * @throws IOException if execution fails
     */
    protected String runScheduledExecutorSchedule(List<Thread> threads) throws IOException {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(Math.max(1, threads.size()));
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (Thread thread : threads) {
                futures.add(executor.schedule(() -> {
                    startThreadAndAwait(thread, "ScheduledExecutorService.schedule for " + thread.getName());
                    return null;
                }, 0, TimeUnit.MILLISECONDS));
            }
            awaitFutures(futures, "ScheduledExecutorService.schedule");
        } finally {
            shutdownExecutor(executor, "ScheduledExecutorService.schedule");
        }
        return "ScheduledExecutorService.schedule via " + describeThreads(threads);
    }

    /**
     * Executes the provided threads using {@link ForkJoinPool#submit(java.util.concurrent.Callable)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param threads threads to run
     * @return textual description of the execution path
     * @throws IOException if execution fails
     */
    protected String runForkJoinPoolSubmit(List<Thread> threads) throws IOException {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        List<ForkJoinTask<Void>> tasks = new ArrayList<>();
        for (Thread thread : threads) {
            tasks.add(pool.submit(() -> {
                startThreadAndAwait(thread, "ForkJoinPool.submit for " + thread.getName());
                return null;
            }));
        }
        joinForkJoinTasks(tasks, "ForkJoinPool.submit");
        return "ForkJoinPool.submit via " + describeThreads(threads);
    }

    /**
     * Executes the provided threads using {@link CompletableFuture#runAsync(Runnable)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param threads threads to run
     * @return textual description of the execution path
     * @throws IOException if execution fails
     */
    protected String runCompletableFutureRunAsync(List<Thread> threads) throws IOException {
        List<CompletableFuture<Void>> futures = threads.stream()
                .map(thread -> CompletableFuture.runAsync(() -> {
                    try {
                        startThreadAndAwait(thread, "CompletableFuture.runAsync for " + thread.getName());
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                }))
                .toList();
        joinCompletableFutures(futures, "CompletableFuture.runAsync");
        return "CompletableFuture.runAsync via " + describeThreads(threads);
    }

    /**
     * Executes the provided threads using {@link CompletableFuture#supplyAsync(java.util.function.Supplier)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param threads threads to run
     * @return textual description of the execution path
     * @throws IOException if execution fails
     */
    protected String runCompletableFutureSupplyAsync(List<Thread> threads) throws IOException {
        List<CompletableFuture<String>> futures = threads.stream()
                .map(thread -> CompletableFuture.supplyAsync(() -> {
                    try {
                        startThreadAndAwait(thread, "CompletableFuture.supplyAsync for " + thread.getName());
                        return thread.getName();
                    } catch (IOException e) {
                        throw new CompletionException(e);
                    }
                }))
                .toList();
        for (CompletableFuture<String> future : futures) {
            joinCompletableFuture(future, "CompletableFuture.supplyAsync");
        }
        return "CompletableFuture.supplyAsync via " + describeThreads(threads);
    }

    /**
     * Creates a string describing a connection target and port.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param description textual descriptor of the connection
     * @param port port number used
     * @return formatted description string
     */
    protected String describePort(String description, int port) {
        return String.format("%s@%d", description, port);
    }

    /**
     * Creates a string describing an operation result.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param description textual descriptor of the operation
     * @param result result captured from the operation
     * @return formatted description string
     */
    protected String describeResult(String description, String result) {
        return String.format("%s -> %s", description, result);
    }

    /**
     * Opens a blocking socket connection to the provided host and port.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @throws IOException if the socket cannot be established
     */
    protected void openSocket(String host, int port) throws IOException {
        try (Socket socket = new Socket(host, port)) {
            // connection established
        }
    }

    /**
     * Opens a blocking socket connection using an InetAddress.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param address remote address to connect to
     * @param port port to connect to
     * @throws IOException if the socket cannot be established
     */
    protected void openSocket(InetAddress address, int port) throws IOException {
        try (Socket socket = new Socket(address, port)) {
            // connection established
        }
    }

    /**
     * Connects a socket created via the no-arg constructor.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @throws IOException if the socket cannot be established
     */
    protected void connectWithExplicitSocket(String host, int port) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port));
        }
    }

    /**
     * Opens a socket using the default SocketFactory.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @throws IOException if the socket cannot be established
     */
    protected void openSocketFromFactory(String host, int port) throws IOException {
        try (Socket socket = SocketFactory.getDefault().createSocket(host, port)) {
            // connection established
        }
    }

    /**
     * Opens a SocketChannel to the provided host and port.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @throws IOException if the channel cannot be opened
     */
    protected void openSocketChannel(String host, int port) throws IOException {
        try (SocketChannel channel = SocketChannel.open(new InetSocketAddress(host, port))) {
            // connection established
        }
    }

    /**
     * Opens an AsynchronousSocketChannel to the provided host and port.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @throws IOException if the channel cannot be opened
     */
    protected void openAsyncSocketChannel(String host, int port) throws IOException {
        try (AsynchronousSocketChannel channel = AsynchronousSocketChannel.open()) {
            connectAsync(channel, host, port);
        }
    }

    /**
     * Connects the provided asynchronous channel and waits for completion.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param channel asynchronous channel to connect
     * @param host host to connect to
     * @param port port to connect to
     * @throws IOException if the connection fails or is interrupted
     */
    protected void connectAsync(AsynchronousSocketChannel channel, String host, int port) throws IOException {
        try {
            channel.connect(new InetSocketAddress(host, port)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while connecting asynchronously", e);
        } catch (ExecutionException e) {
            throw new IOException("Asynchronous connect failed", e.getCause());
        }
    }

    /**
     * Reads bytes from the input stream of a blocking socket.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @return decoded UTF-8 payload
     * @throws IOException if the read fails
     */
    protected String readFromSocketInputStream(String host, int port) throws IOException {
        try (Socket socket = new Socket(host, port); InputStream in = socket.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Reads a line of text from a socket via BufferedReader.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @return decoded UTF-8 payload
     * @throws IOException if the read fails
     */
    protected String readWithBufferedReader(String host, int port) throws IOException {
        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(),
                     StandardCharsets.UTF_8))) {
            return reader.readLine();
        }
    }

    /**
     * Reads bytes from a SocketChannel into a buffer sized for the payload.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @param payloadSize number of bytes expected from the server
     * @return decoded UTF-8 payload
     * @throws IOException if the read fails
     */
    protected String readWithSocketChannel(String host, int port, int payloadSize) throws IOException {
        try (SocketChannel channel = SocketChannel.open(new InetSocketAddress(host, port))) {
            return readFromChannel(channel, payloadSize);
        }
    }

    /**
     * Reads bytes from an AsynchronousSocketChannel.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @param payloadSize number of bytes expected from the server
     * @return decoded UTF-8 payload
     * @throws IOException if the read fails or is interrupted
     */
    protected String readWithAsynchronousSocketChannel(String host, int port, int payloadSize) throws IOException {
        try (AsynchronousSocketChannel channel = AsynchronousSocketChannel.open()) {
            ByteBuffer buffer = ByteBuffer.allocate(payloadSize);
            try {
                channel.connect(new InetSocketAddress(host, port)).get();
                channel.read(buffer).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while receiving asynchronously", e);
            } catch (ExecutionException e) {
                throw new IOException("Asynchronous receive failed", e.getCause());
            }
            buffer.flip();
            return decodeBuffer(buffer);
        }
    }

    /**
     * Reads bytes from a DatagramSocket.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @param payloadSize number of bytes to allocate for the response buffer
     * @return decoded UTF-8 payload
     * @throws IOException if the read fails
     */
    protected String readWithDatagramSocket(String host, int port, int payloadSize) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(new InetSocketAddress(host, port));
            socket.send(new DatagramPacket(new byte[]{0}, 1));
            byte[] buffer = new byte[payloadSize];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            return new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Reads bytes from a DatagramChannel.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @param payloadSize number of bytes to allocate for the response buffer
     * @return decoded UTF-8 payload
     * @throws IOException if the read fails
     */
    protected String readWithDatagramChannel(String host, int port, int payloadSize) throws IOException {
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.connect(new InetSocketAddress(host, port));
            channel.write(ByteBuffer.wrap(new byte[]{0}));
            ByteBuffer buffer = ByteBuffer.allocate(payloadSize);
            channel.read(buffer);
            buffer.flip();
            return decodeBuffer(buffer);
        }
    }

    /**
     * Sends bytes via a blocking socket's output stream.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @param payload bytes to send
     * @throws IOException if sending fails
     */
    protected void writeWithSocketOutputStream(String host, int port, byte[] payload) throws IOException {
        try (Socket socket = new Socket(host, port); OutputStream out = socket.getOutputStream()) {
            out.write(payload);
            out.flush();
        }
    }

    /**
     * Sends text via a buffered writer wrapping a socket output stream.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @param message message to send
     * @throws IOException if sending fails
     */
    protected void writeWithBufferedWriter(String host, int port, String message) throws IOException {
        try (Socket socket = new Socket(host, port);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),
                     StandardCharsets.UTF_8))) {
            writer.write(message);
            writer.flush();
        }
    }

    /**
     * Sends bytes via {@link SocketChannel#write(ByteBuffer)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @param payload bytes to send
     * @throws IOException if sending fails
     */
    protected void writeWithSocketChannel(String host, int port, byte[] payload) throws IOException {
        try (SocketChannel channel = SocketChannel.open(new InetSocketAddress(host, port))) {
            channel.write(ByteBuffer.wrap(payload));
        }
    }

    /**
     * Sends bytes via {@link AsynchronousSocketChannel#write(ByteBuffer)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @param payload bytes to send
     * @throws IOException if sending fails or is interrupted
     */
    protected void writeWithAsynchronousSocketChannel(String host, int port, byte[] payload) throws IOException {
        try (AsynchronousSocketChannel channel = AsynchronousSocketChannel.open()) {
            try {
                channel.connect(new InetSocketAddress(host, port)).get();
                channel.write(ByteBuffer.wrap(payload)).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while sending asynchronously", e);
            } catch (ExecutionException e) {
                throw new IOException("Asynchronous send failed", e.getCause());
            }
        }
    }

    /**
     * Sends bytes via {@link DatagramSocket#send(DatagramPacket)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @param payload bytes to send
     * @throws IOException if sending fails
     */
    protected void writeWithDatagramSocket(String host, int port, byte[] payload) throws IOException {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(new InetSocketAddress(host, port));
            DatagramPacket packet = new DatagramPacket(payload, payload.length);
            socket.send(packet);
        }
    }

    /**
     * Sends bytes via {@link DatagramChannel#write(ByteBuffer)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param host host to connect to
     * @param port port to connect to
     * @param payload bytes to send
     * @throws IOException if sending fails
     */
    protected void writeWithDatagramChannel(String host, int port, byte[] payload) throws IOException {
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.connect(new InetSocketAddress(host, port));
            channel.write(ByteBuffer.wrap(payload));
        }
    }

    /**
     * Reads bytes from the provided channel into a buffer and decodes it.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param channel readable channel supplying bytes
     * @param payloadSize number of bytes expected
     * @return decoded UTF-8 payload
     * @throws IOException if reading fails
     */
    protected String readFromChannel(ReadableByteChannel channel, int payloadSize) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(payloadSize);
        channel.read(buffer);
        buffer.flip();
        return decodeBuffer(buffer);
    }

    /**
     * Decodes the remaining bytes in the buffer as UTF-8 text.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param buffer buffer containing the bytes to decode
     * @return decoded UTF-8 payload
     */
    protected String decodeBuffer(ByteBuffer buffer) {
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Accepts inbound TCP connections on the loopback interface.
     *
     * <p>Description: Used to verify outbound network operations by draining
     * whatever data the client sends and exposing connection metadata.
     *
     * <p>Design Rationale: Keeps network scaffolding localized and reusable
     * across multiple access classes.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @version 0.0.1
     */
    protected static class LoopbackTcpAcceptServer implements AutoCloseable {

        /**
         * Underlying server socket listening on loopback.
         */
        private final ServerSocket server;

        /**
         * Latch released when the server has accepted and processed a client.
         */
        private final CountDownLatch latch;

        /**
         * Creates and starts the loopback server.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @param threadName name assigned to the server thread
         * @throws IOException if the server socket cannot be created
         */
        protected LoopbackTcpAcceptServer(String threadName) throws IOException {
            this.server = new ServerSocket(0, 1, LOOPBACK);
            this.latch = new CountDownLatch(1);
            Thread thread = new Thread(this::acceptAndDrain, threadName);
            thread.start();
        }

        /**
         * Accepts a single connection and drains any sent bytes.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         */
        private void acceptAndDrain() {
            try (Socket socket = server.accept()) {
                socket.getInputStream().readAllBytes();
            } catch (IOException ignored) {
            } finally {
                latch.countDown();
            }
        }

        /**
         * Returns the host serving the connections.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @return loopback host string
         */
        protected String host() {
            return LOOPBACK_HOST;
        }

        /**
         * Returns the dynamically assigned port number.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @return bound port number
         */
        protected int port() {
            return server.getLocalPort();
        }

        /**
         * Returns the latch signaling when the server has processed a client.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @return latch signaled by the server thread
         */
        protected CountDownLatch latch() {
            return latch;
        }

        /**
         * Stops the server socket and releases any waiting clients.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         */
        @Override
        public void close() throws IOException {
            server.close();
        }
    }

    /**
     * Sends a predefined payload to TCP clients connecting via loopback.
     *
     * <p>Description: Accepts a single client and writes the configured payload
     * before closing, allowing receive demonstrations to capture the data.
     *
     * <p>Design Rationale: Provides deterministic responses so tests can focus
     * on exercising the network APIs rather than server logic.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @version 0.0.1
     */
    protected static class LoopbackTcpPayloadServer implements AutoCloseable {

        /**
         * Server socket bound to loopback for payload delivery.
         */
        private final ServerSocket server;

        /**
         * Latch released when the payload has been sent.
         */
        private final CountDownLatch latch;

        /**
         * Payload sent to any connected client.
         */
        private final byte[] payload;

        /**
         * Creates a payload server with the provided payload.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @param payload payload to send to connected clients
         * @throws IOException if the server socket cannot be created
         */
        protected LoopbackTcpPayloadServer(byte[] payload) throws IOException {
            this.server = new ServerSocket(0, 1, LOOPBACK);
            this.latch = new CountDownLatch(1);
            this.payload = payload.clone();
            Thread thread = new Thread(this::acceptAndSend, "tcp-receive-server");
            thread.start();
        }

        /**
         * Accepts a client, sends the payload, and counts down the latch.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         */
        private void acceptAndSend() {
            try (Socket socket = server.accept()) {
                socket.getOutputStream().write(payload);
                socket.shutdownOutput();
            } catch (IOException ignored) {
            } finally {
                latch.countDown();
            }
        }

        /**
         * Returns the loopback host string.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @return loopback host string
         */
        protected String host() {
            return LOOPBACK_HOST;
        }

        /**
         * Returns the dynamically assigned port number.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @return bound port number
         */
        protected int port() {
            return server.getLocalPort();
        }

        /**
         * Returns the latch signaling payload delivery.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @return latch signaling payload delivery
         */
        protected CountDownLatch latch() {
            return latch;
        }

        /**
         * Closes the underlying server socket.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         */
        @Override
        public void close() throws IOException {
            server.close();
        }
    }

    /**
     * Receives UDP packets and signals completion once a payload arrives.
     *
     * <p>Description: Listens on loopback for a single datagram and notifies
     * callers when data has been consumed.
     *
     * <p>Design Rationale: Serves as the sink for outbound UDP send
     * demonstrations without requiring additional infrastructure.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @version 0.0.1
     */
    protected static class LoopbackUdpSinkServer implements AutoCloseable {

        /**
         * Datagram socket bound to loopback.
         */
        private final DatagramSocket socket;

        /**
         * Latch signaling that a datagram has been received.
         */
        private final CountDownLatch latch;

        /**
         * Expected payload size used to size receive buffers.
         */
        private final int payloadSize;

        /**
         * Creates the UDP sink server.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @param payloadSize expected size of incoming payloads
         * @throws IOException if the socket cannot be bound
         */
        protected LoopbackUdpSinkServer(int payloadSize) throws IOException {
            this.socket = new DatagramSocket(0, LOOPBACK);
            this.latch = new CountDownLatch(1);
            this.payloadSize = payloadSize;
            Thread thread = new Thread(this::receivePacket, "udp-send-server");
            thread.start();
        }

        /**
         * Receives a single datagram and counts down the latch.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         */
        private void receivePacket() {
            try {
                byte[] buffer = new byte[payloadSize];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
            } catch (IOException ignored) {
            } finally {
                latch.countDown();
            }
        }

        /**
         * Returns the loopback host string.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @return loopback host string
         */
        protected String host() {
            return LOOPBACK_HOST;
        }

        /**
         * Returns the dynamically assigned port.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @return bound port number
         */
        protected int port() {
            return socket.getLocalPort();
        }

        /**
         * Returns the latch signaled when a packet arrives.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @return latch used to synchronize with writers
         */
        protected CountDownLatch latch() {
            return latch;
        }

        /**
         * Closes the UDP socket.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         */
        @Override
        public void close() {
            socket.close();
        }
    }

    /**
     * Sends a predefined payload in response to UDP clients.
     *
     * <p>Description: Receives an initial datagram and responds with the
     * configured payload, enabling receive demonstrations.
     *
     * <p>Design Rationale: Mirrors the TCP payload server for UDP transport to
     * keep tests symmetric.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @version 0.0.1
     */
    protected static class LoopbackUdpPayloadServer implements AutoCloseable {

        /**
         * Datagram socket bound to loopback.
         */
        private final DatagramSocket socket;

        /**
         * Latch released when the payload has been sent.
         */
        private final CountDownLatch latch;

        /**
         * Payload sent back to clients.
         */
        private final byte[] payload;

        /**
         * Creates the UDP payload server.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @param payload payload to send in response
         * @throws IOException if the socket cannot be bound
         */
        protected LoopbackUdpPayloadServer(byte[] payload) throws IOException {
            this.socket = new DatagramSocket(0, LOOPBACK);
            this.latch = new CountDownLatch(1);
            this.payload = payload.clone();
            Thread thread = new Thread(this::respondWithPayload, "udp-receive-server");
            thread.start();
        }

        /**
         * Responds to the first client and counts down the latch.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         */
        private void respondWithPayload() {
            try {
                byte[] buffer = new byte[1];
                DatagramPacket registration = new DatagramPacket(buffer, buffer.length);
                socket.receive(registration);
                DatagramPacket response = new DatagramPacket(payload, payload.length,
                        registration.getAddress(), registration.getPort());
                socket.send(response);
            } catch (IOException ignored) {
            } finally {
                latch.countDown();
            }
        }

        /**
         * Returns the loopback host string.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @return loopback host string
         */
        protected String host() {
            return LOOPBACK_HOST;
        }

        /**
         * Returns the dynamically assigned port number.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @return bound port number
         */
        protected int port() {
            return socket.getLocalPort();
        }

        /**
         * Returns the latch signaling that the payload has been sent.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @return latch used for synchronization
         */
        protected CountDownLatch latch() {
            return latch;
        }

        /**
         * Closes the UDP socket.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         */
        @Override
        public void close() {
            socket.close();
        }
    }
}
