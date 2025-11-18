package de.tum.cit.aet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * Demonstrates creating and starting threads via numerous Java concurrency APIs.
 *
 * <p>Description: Exercises thread instantiation via raw Thread,
 * executor services, parallel streams, and the Arrays parallel helpers to show
 * how each mechanism interacts with configured resources.
 *
 * <p>Design Rationale: Consolidates the thread creation flows into one class so
 * auditing the supported mechanisms becomes straightforward.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
public class ThreadSystemCreateAccess extends ProtectedRessourceAccess {

    /**
     * Number of creation flows supported by this class.
     */
    private static final int AMOUNT_OF_METHODS = 12;

    /**
     * Reports the number of thread creation flows provided.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return total thread creation demonstrations
     */
    public static int getAmountOfMethods() {
        return AMOUNT_OF_METHODS;
    }

    /**
     * Lists the fully qualified thread and runnable class names.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @return immutable list containing the thread and runnable class names
     */
    @Override
    public List<String> listHandeledRessources() {
        return List.of("de.tum.cit.aet.ThreadToCreate", "de.tum.cit.aet.RunnableToCreate");
    }

    /**
     * Creates localized success and failure messages for thread creation.
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
                String.format("Successfully triggered thread creation on %s%s", resource, suffix),
                String.format("Failed to trigger thread creation on %s for operation id %s", resource, id));
    }

    /**
     * Executes the requested thread creation flow by id.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param id identifier of the thread creation strategy to run
     * @return formatted status message describing the result
     * @throws IOException if starting or joining the thread fails
     */
    @Override
    public String accessProtectedRessourceById(int id) throws IOException {
        List<String> resources = listHandeledRessources();
        if (resources.isEmpty()) {
            throw new IOException("No thread classes configured");
        }
        String resource = resources.get(0);
        return switch (id) {
            case 1 -> createWithThreadStart(resource);
            case 2 -> createWithExecutorExecute(resource);
            case 3 -> createWithExecutorServiceSubmit(resource);
            case 4 -> createWithThreadPoolExecutorSubmit(resource);
            case 5 -> createWithScheduledExecutorSchedule(resource);
            case 6 -> createWithForkJoinPoolSubmit(resource);
            case 7 -> createWithCompletableFutureRunAsync(resource);
            case 8 -> createWithCompletableFutureSupplyAsync(resource);
            case 9 -> createWithCollectionParallelStream(resource);
            case 10 -> createWithStreamParallel(resource);
            case 11 -> createWithArraysParallelSort(resource);
            case 12 -> createWithArraysParallelPrefix(resource);
            default -> failure(resource, id);
        };
    }

    /**
     * Creates and starts a thread via {@link Thread#start()}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the target class
     * @return success message describing the used thread
     * @throws IOException if the thread fails to start or join
     */
    private String createWithThreadStart(String resource) throws IOException {
        Thread thread = instantiateThreads();
        startThreadAndAwait(thread, "Thread.start for " + thread.getName());
        return success(resource, "Thread.start via " + thread.getName());
    }

    /**
     * Creates a thread via {@link ExecutorService#execute(Runnable)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the target class
     * @return success message describing the used executor
     * @throws IOException if execution fails
     */
    private String createWithExecutorExecute(String resource) throws IOException {
        Thread thread = instantiateThreads();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<IOException> failure = new AtomicReference<>();
        try {
            executor.execute(() -> runExecutorTask(thread, "Executor.execute for " + thread.getName(), failure, latch));
            awaitLatch(latch, "Executor.execute");
            if (failure.get() != null) {
                throw failure.get();
            }
            return success(resource, "Executor.execute via " + thread.getName());
        } finally {
            shutdownExecutor(executor, "Executor.execute");
        }
    }

    /**
     * Creates a thread via {@link ExecutorService#submit(java.util.concurrent.Callable)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the target class
     * @return success message describing the used executor
     * @throws IOException if execution fails
     */
    private String createWithExecutorServiceSubmit(String resource) throws IOException {
        Thread thread = instantiateThreads();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            List<Future<Void>> futures = new ArrayList<>();
            futures.add(executor.submit(() -> {
                startThreadAndAwait(thread, "ExecutorService.submit for " + thread.getName());
                return null;
            }));
            awaitFutures(futures, "ExecutorService.submit");
            return success(resource, "ExecutorService.submit via " + thread.getName());
        } finally {
            shutdownExecutor(executor, "ExecutorService.submit");
        }
    }

    /**
     * Creates a thread via {@link ThreadPoolExecutor#submit(Runnable)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the target class
     * @return success message describing the used executor
     * @throws IOException if execution fails
     */
    private String createWithThreadPoolExecutorSubmit(String resource) throws IOException {
        Thread thread = instantiateThreads();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
        try {
            List<Future<Void>> futures = new ArrayList<>();
            futures.add(executor.submit(() -> {
                startThreadAndAwait(thread, "ThreadPoolExecutor.submit for " + thread.getName());
                return null;
            }));
            awaitFutures(futures, "ThreadPoolExecutor.submit");
            return success(resource, "ThreadPoolExecutor.submit via " + thread.getName());
        } finally {
            shutdownExecutor(executor, "ThreadPoolExecutor.submit");
        }
    }

    /**
     * Creates a thread via {@link ScheduledExecutorService#schedule(Runnable, long, TimeUnit)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the target class
     * @return success message describing the used executor
     * @throws IOException if execution fails
     */
    private String createWithScheduledExecutorSchedule(String resource) throws IOException {
        Thread thread = instantiateThreads();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            List<Future<Void>> futures = new ArrayList<>();
            futures.add(executor.schedule(() -> {
                startThreadAndAwait(thread, "ScheduledExecutorService.schedule for " + thread.getName());
                return null;
            }, 0, TimeUnit.MILLISECONDS));
            awaitFutures(futures, "ScheduledExecutorService.schedule");
            return success(resource, "ScheduledExecutorService.schedule via " + thread.getName());
        } finally {
            shutdownExecutor(executor, "ScheduledExecutorService.schedule");
        }
    }

    /**
     * Creates a thread via {@link ForkJoinPool#submit(java.util.concurrent.Callable)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the target class
     * @return success message describing the used pool
     * @throws IOException if execution fails
     */
    private String createWithForkJoinPoolSubmit(String resource) throws IOException {
        Thread thread = instantiateThreads();
        ForkJoinPool pool = ForkJoinPool.commonPool();
        List<ForkJoinTask<Void>> tasks = new ArrayList<>();
        tasks.add(pool.submit(() -> {
            startThreadAndAwait(thread, "ForkJoinPool.submit for " + thread.getName());
            return null;
        }));
        joinForkJoinTasks(tasks, "ForkJoinPool.submit");
        return success(resource, "ForkJoinPool.submit via " + thread.getName());
    }

    /**
     * Creates a thread via {@link CompletableFuture#runAsync(Runnable)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the target class
     * @return success message describing the used future
     * @throws IOException if execution fails
     */
    private String createWithCompletableFutureRunAsync(String resource) throws IOException {
        Thread thread = instantiateThreads();
        List<CompletableFuture<Void>> futures = List.of(
                CompletableFuture.runAsync(() -> runUnchecked(thread, "CompletableFuture.runAsync for " + thread.getName())));
        joinCompletableFutures(futures, "CompletableFuture.runAsync");
        return success(resource, "CompletableFuture.runAsync via " + thread.getName());
    }

    /**
     * Creates a thread via {@link CompletableFuture#supplyAsync(java.util.function.Supplier)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the target class
     * @return success message describing the used future
     * @throws IOException if execution fails
     */
    private String createWithCompletableFutureSupplyAsync(String resource) throws IOException {
        Thread thread = instantiateThreads();
        List<CompletableFuture<String>> futures = List.of(CompletableFuture.supplyAsync(() -> {
            runUnchecked(thread, "CompletableFuture.supplyAsync for " + thread.getName());
            return thread.getName();
        }));
        for (CompletableFuture<String> future : futures) {
            joinCompletableFuture(future, "CompletableFuture.supplyAsync");
        }
        return success(resource, "CompletableFuture.supplyAsync via " + thread.getName());
    }

    /**
     * Creates a thread via {@link List#parallelStream()}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the target class
     * @return success message describing the used stream
     * @throws IOException if execution fails
     */
    private String createWithCollectionParallelStream(String resource) throws IOException {
        Thread thread = instantiateThreads();
        List.of(thread).parallelStream().forEach(t -> runUnchecked(t, "Collection.parallelStream for " + t.getName()));
        return success(resource, "Collection.parallelStream via " + thread.getName());
    }

    /**
     * Creates a thread via {@link Stream#parallel()}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the target class
     * @return success message describing the used stream
     * @throws IOException if execution fails
     */
    private String createWithStreamParallel(String resource) throws IOException {
        Thread thread = instantiateThreads();
        Stream.of(thread).parallel().forEach(t -> runUnchecked(t, "Stream.parallel for " + t.getName()));
        return success(resource, "Stream.parallel via " + thread.getName());
    }

    /**
     * Demonstrates the relationship between thread creation and {@link Arrays#parallelSort(int[])}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the target class
     * @return success message describing the sorted values
     * @throws IOException if execution fails
     */
    private String createWithArraysParallelSort(String resource) throws IOException {
        Thread thread = instantiateThreads();
        startThreadAndAwait(thread, "Arrays.parallelSort pre-start for " + thread.getName());
        int[] values = {3, 1, 2};
        Arrays.parallelSort(values);
        return success(resource, "Arrays.parallelSort -> " + Arrays.toString(values) + " via " + thread.getName());
    }

    /**
     * Demonstrates the relationship between thread creation and {@link Arrays#parallelPrefix(int[], java.util.function.IntBinaryOperator)}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param resource textual description of the target class
     * @return success message describing the prefixed values
     * @throws IOException if execution fails
     */
    private String createWithArraysParallelPrefix(String resource) throws IOException {
        Thread thread = instantiateThreads();
        startThreadAndAwait(thread, "Arrays.parallelPrefix pre-start for " + thread.getName());
        int[] values = {1, 2, 3};
        Arrays.parallelPrefix(values, Integer::sum);
        return success(resource, "Arrays.parallelPrefix -> " + Arrays.toString(values) + " via " + thread.getName());
    }

    /**
     * Executes the given thread inside an executor task while capturing failures.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param thread thread being executed
     * @param context textual description of the execution context
     * @param failure shared reference that stores the first failure
     * @param latch latch released when the task completes
     */
    private void runExecutorTask(Thread thread, String context, AtomicReference<IOException> failure,
            CountDownLatch latch) {
        try {
            startThreadAndAwait(thread, context);
        } catch (IOException e) {
            failure.compareAndSet(null, e);
            sneakyThrow(e);
        } finally {
            latch.countDown();
        }
    }

    /**
     * Starts the given thread and propagates errors as unchecked exceptions.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param thread thread being executed
     * @param context textual description of the execution context
     */
    private void runUnchecked(Thread thread, String context) {
        try {
            startThreadAndAwait(thread, context);
        } catch (IOException e) {
            sneakyThrow(e);
        }
    }

    /**
     * Throws the provided exception without declaring it.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param throwable exception to rethrow
     * @param <T> inferred exception type
     */
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable throwable) throws T {
        throw (T) throwable;
    }
}
