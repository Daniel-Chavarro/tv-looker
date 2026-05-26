package org.tvl.tvlooker.service.tmdb.support;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

public record TmdbExecutorSnapshot(
        int poolSize,
        int activeCount,
        int queueSize,
        long completedTaskCount,
        int corePoolSize,
        int maxPoolSize,
        int largestPoolSize,
        boolean queuePresent
) {
    public static TmdbExecutorSnapshot capture(ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor nativeExecutor = executor.getThreadPoolExecutor();
        return new TmdbExecutorSnapshot(
                executor.getPoolSize(),
                executor.getActiveCount(),
                executor.getQueueSize(),
                nativeExecutor == null ? 0L : nativeExecutor.getCompletedTaskCount(),
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                nativeExecutor == null ? 0 : nativeExecutor.getLargestPoolSize(),
                nativeExecutor != null
        );
    }
}