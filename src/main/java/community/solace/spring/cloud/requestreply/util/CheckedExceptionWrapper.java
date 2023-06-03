/*
 * Copyright © Schweizerische Bundesbahnen SBB, 2021.
 */

package community.solace.spring.cloud.requestreply.util;

import java.util.function.Function;
import java.util.function.Supplier;

import community.solace.spring.cloud.requestreply.exception.RequestReplyException;

public final class CheckedExceptionWrapper {
    private CheckedExceptionWrapper() {
        // static methods only
    }

    public static <T, R, E extends Exception> Function<T, R> throwingUnchecked(FunctionWithException<T, R, E> checkedExceptionFunction) {
        return arg -> {
            try {
                return checkedExceptionFunction.apply(arg);
            }
            catch (Exception e) {
                throw new RequestReplyException(e);
            }
        };
    }

    public static <E extends Exception> Runnable throwingUnchecked(RunnableWithException<E> checkedExceptionRunnable) {
        return () -> {
            try {
                checkedExceptionRunnable.run();
            }
            catch (Exception e) {
                throw new RequestReplyException(e);
            }
        };
    }

    public static <T, E extends Exception> Supplier<T> throwingUnchecked(SupplierWithException<T, E> checkedExceptionSupplier) {
        return () -> {
            try {
                return checkedExceptionSupplier.get();
            }
            catch (Exception e) {
                throw new RequestReplyException(e);
            }
        };
    }

    @FunctionalInterface
    public interface FunctionWithException<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    @FunctionalInterface
    public interface RunnableWithException<E extends Exception> {
        void run() throws E;
    }

    @FunctionalInterface
    public interface SupplierWithException<T, E extends Exception> {
        T get() throws E;
    }
}