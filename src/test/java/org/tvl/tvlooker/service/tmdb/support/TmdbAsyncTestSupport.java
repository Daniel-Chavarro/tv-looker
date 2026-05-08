package org.tvl.tvlooker.service.tmdb.support;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class TmdbAsyncTestSupport {
    private TmdbAsyncTestSupport() {
    }

    public static <T> CompletableFuture<T> completed(T value) {
        return completedFuture(value);
    }

    public static <T> CompletableFuture<T> completedFuture(T value) {
        return CompletableFuture.completedFuture(value);
    }

    public static <T> CompletableFuture<T> failed(Throwable error) {
        return failedFuture(error);
    }

    public static <T> CompletableFuture<T> failedFuture(Throwable error) {
        Objects.requireNonNull(error, "error");
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(error);
        return future;
    }

    public static <T> CompletableFuture<T> neverCompleting() {
        return stuckFuture();
    }

    public static <T> CompletableFuture<T> stuckFuture() {
        return new CompletableFuture<>();
    }

    public static <T> CompletableFuture<T> delayed(T value, Duration delay, Executor executor) {
        return delayedCompletion(value, delay, executor);
    }

    public static <T> CompletableFuture<T> delayedCompletion(T value, Duration delay, Executor executor) {
        Objects.requireNonNull(delay, "delay");
        Objects.requireNonNull(executor, "executor");
        return CompletableFuture.supplyAsync(() -> value,
                CompletableFuture.delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS, executor));
    }

    public static <T> CompletableFuture<T> delayed(T value, Duration delay, ScheduledExecutorService scheduler) {
        return delayedCompletion(value, delay, scheduler);
    }

    public static <T> CompletableFuture<T> delayedCompletion(T value, Duration delay, ScheduledExecutorService scheduler) {
        Objects.requireNonNull(delay, "delay");
        Objects.requireNonNull(scheduler, "scheduler");
        CompletableFuture<T> future = new CompletableFuture<>();
        scheduler.schedule(() -> future.complete(value), delay.toMillis(), TimeUnit.MILLISECONDS);
        return future;
    }

    public static CompletableFuture<Void> blocking(CountDownLatch started, CountDownLatch release, Executor executor) {
        Objects.requireNonNull(started, "started");
        Objects.requireNonNull(release, "release");
        Objects.requireNonNull(executor, "executor");
        return CompletableFuture.runAsync(() -> {
            started.countDown();
            awaitUnchecked(release);
        }, executor);
    }

    public static <T> CompletableFuture<T> blockingValue(T value, CountDownLatch started, CountDownLatch release,
            Executor executor) {
        Objects.requireNonNull(started, "started");
        Objects.requireNonNull(release, "release");
        Objects.requireNonNull(executor, "executor");
        return CompletableFuture.supplyAsync(() -> {
            started.countDown();
            awaitUnchecked(release);
            return value;
        }, executor);
    }

    public static Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException completionException && completionException.getCause() != null) {
            return completionException.getCause();
        }
        return throwable;
    }

    private static void awaitUnchecked(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new CompletionException(new TimeoutException("Latch did not release in time"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        }
    }
}