package org.tvl.tvlooker.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for asynchronous task execution.
 * Provides dedicated thread pools for background operations.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-15
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfiguration {

    /**
     * Defines a TaskExecutor bean with a thread pool configured based on available processors.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(20 * 2);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("Tasks-Async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool executor for TMDB API calls.
     *
     * @return configured Executor bean
     */
    @Bean(name = "tmdbTaskExecutor")
    public Executor tmdbTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core threads: number of threads to keep alive even if idle
        executor.setCorePoolSize(20);

        // Max threads: maximum number of threads
        executor.setMaxPoolSize(40);

        // Queue size: pending tasks when all threads are busy
        executor.setQueueCapacity(200);

        // Thread naming for debugging
        executor.setThreadNamePrefix("tmdb-fetch-");

        // When queue is full, run the task in the caller's thread
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }

    /**
     * Thread pool executor for TMDB collection and synchronization orchestration.
     *
     * @return configured Executor bean
     */
    @Bean(name = "tmdbOrchestrationExecutor")
    public Executor tmdbOrchestrationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("tmdb-orchestration-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
