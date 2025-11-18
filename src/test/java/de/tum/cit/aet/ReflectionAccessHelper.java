package de.tum.cit.aet;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Reflection utilities for interacting with access classes under test.
 *
 * <p>Description: Provides helper methods that instantiate access classes, invoke their APIs, and
 * capture results without duplicating boilerplate across tests.
 *
 * <p>Design Rationale: Centralizes reflective operations to keep the runner tests concise and
 * consistent.
 *
 * @since 0.0.1
 * @author Markus Paulsen
 * @version 0.0.1
 */
final class ReflectionAccessHelper {

    /**
     * Hidden constructor to prevent instantiation.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     */
    private ReflectionAccessHelper() {
    }

    /**
     * Instantiates the given class by its fully qualified name.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param className name of the class to instantiate
     * @return new instance created via the default constructor
     * @throws Exception if instantiation fails
     */
    static Object instantiate(String className) throws Exception {
        try {
            return Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException e) {
            throw rethrowCause(e);
        }
    }

    /**
     * Retrieves the list of handled resources from an access instance.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param instance access class instance
     * @return immutable list of resource descriptions
     * @throws Exception if invocation fails
     */
    static List<String> listHandledResources(Object instance) throws Exception {
        try {
            @SuppressWarnings("unchecked")
            List<String> resources = (List<String>) instance.getClass()
                    .getMethod("listHandeledRessources")
                    .invoke(instance);
            return resources;
        } catch (InvocationTargetException e) {
            throw rethrowCause(e);
        }
    }

    /**
     * Invokes {@code accessProtectedRessourceById} on the provided instance.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param instance access instance
     * @param id method identifier
     * @return message returned by the access method
     * @throws Exception if invocation fails
     */
    static String accessById(Object instance, int id) throws Exception {
        try {
            return (String) instance.getClass()
                    .getMethod("accessProtectedRessourceById", int.class)
                    .invoke(instance, id);
        } catch (InvocationTargetException e) {
            throw rethrowCause(e);
        }
    }

    /**
     * Invokes the access method and captures success or failure metadata.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param instance access instance
     * @param id method identifier
     * @return invocation result containing message/exception details
     */
    static InvocationResult invokeAndCapture(Object instance, int id) {
        try {
            return InvocationResult.success(accessById(instance, id));
        } catch (Exception e) {
            return InvocationResult.failure(e);
        }
    }

    /**
     * Loads the configured amount of methods from the supplied class.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param className access class name
     * @return number of supported method ids
     */
    static int getAmountOfMethods(String className) {
        try {
            return (int) Class.forName(className)
                    .getMethod("getAmountOfMethods")
                    .invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to obtain method count for " + className, e);
        }
    }

    /**
     * Rethrows the root cause of an {@link InvocationTargetException}.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @param e invocation exception
     * @return nothing, always throws
     * @throws Exception when the underlying cause is an {@link Exception}
     */
    private static Exception rethrowCause(InvocationTargetException e) throws Exception {
        Throwable cause = e.getCause();
        if (cause instanceof Exception ex) {
            throw ex;
        }
        throw new Exception("Invocation target failure", cause);
    }

    /**
     * Records the outcome of a reflective invocation.
     *
     * <p>Description: Holds the success flag, resulting message, and exception (if any).
     *
     * <p>Design Rationale: Simplifies assertions inside runner tests by encapsulating metadata.
     *
     * @since 0.0.1
     * @author Markus Paulsen
     * @version 0.0.1
     */
    static final class InvocationResult {
        /**
         * Indicates whether the invocation succeeded.
         */
        private final boolean success;

        /**
         * Message returned by the access call or derived from exceptions.
         */
        private final String message;

        /**
         * Exception thrown during invocation, if any.
         */
        private final Exception exception;

        /**
         * Constructs an invocation result with explicit values.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @param success flag indicating success
         * @param message message returned by the access call
         * @param exception captured exception, possibly {@code null}
         */
        private InvocationResult(boolean success, String message, Exception exception) {
            this.success = success;
            this.message = message;
            this.exception = exception;
        }

        /**
         * Factory for success results.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @param message success message
         * @return invocation result instance
         */
        static InvocationResult success(String message) {
            return new InvocationResult(true, message, null);
        }

        /**
         * Factory for failure results.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @param exception exception thrown during invocation
         * @return invocation result instance
         */
        static InvocationResult failure(Exception exception) {
            String msg = exception != null && exception.getMessage() != null
                    ? exception.getMessage()
                    : exception == null ? "" : exception.getClass().getSimpleName();
            return new InvocationResult(false, msg, exception);
        }

        /**
         * Indicates whether the invocation succeeded.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @return {@code true} when successful
         */
        boolean succeeded() {
            return success;
        }

        /**
         * Returns the captured message.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @return success or failure message
         */
        String message() {
            return message;
        }

        /**
         * Returns the captured exception, if one was thrown.
         *
         * @since 0.0.1
         * @author Markus Paulsen
         * @return exception or {@code null}
         */
        Exception exception() {
            return exception;
        }
    }
}
