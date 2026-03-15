package org.tvl.tvlooker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

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
public class AsyncConfiguration {

    /**
     * Dedicated thread pool for TMDB data collection tasks.
     * Configured to handle long-running background operations without blocking the main application.
     *
     * @return configured executor for TMDB tasks
     */
    @Bean(name = "tmdbTaskExecutor")
    public Executor tmdbTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("tmdb-async-");
        executor.initialize();
        return executor;
    }
}
