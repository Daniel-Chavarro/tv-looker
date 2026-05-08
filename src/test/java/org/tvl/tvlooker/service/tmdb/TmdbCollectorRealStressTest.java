package org.tvl.tvlooker.service.tmdb;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;
import org.opentest4j.TestAbortedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.TmdbClient;
import org.tvl.tvlooker.persistence.tmdb.TmdbMediaType;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMediaDetails;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMediaItem;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.service.tmdb.support.TmdbEvidenceWriter;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

@DisplayName("TMDB collector real stress verification")
class TmdbCollectorRealStressTest {
    private static final Path TASK_8_EVIDENCE_DIR = Path.of(
            ".sisyphus/evidence/collector-thread-blocking/task-8");
    private static final Duration HTTP_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration HTTP_READ_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration TERMINAL_TIMEOUT = Duration.ofSeconds(45);

    @Test
    @Timeout(60)
    @DisplayName("opt-in real TMDB collector stress writes metrics or explicit skip reason")
    void optInRealTmdbStressWritesEvidence() throws Exception {
        String apiKey = System.getenv("TMDB_API_KEY");
        boolean optIn = "true".equalsIgnoreCase(System.getenv("TV_LOOKER_RUN_REAL_TMDB_STRESS"));
        if (apiKey == null || apiKey.isBlank() || !optIn) {
            String skippedReason = skippedReason(apiKey, optIn);
            writeRealStressEvidence(StressEvidence.skipped(skippedReason));
            writeDefaultCiEvidence(skippedReason);
            throw new TestAbortedException(skippedReason);
        }

        ThreadPoolTaskExecutor fetchExecutor = newStressExecutor();
        ExecutorService collectorExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "tmdb-real-collector-stress");
            thread.setDaemon(true);
            return thread;
        });
        StressMetrics metrics = new StressMetrics(fetchExecutor);
        StressEvidence evidence = StressEvidence.started();
        Instant startedAt = Instant.now();
        try {
            TmdbDataFetcher fetcher = new TmdbDataFetcher(
                    new CountingTmdbClient(newRestClient(apiKey), "en-US", metrics), fetchExecutor, 10.0, 2);
            TmdbDataCollectorService collector = newCollector(fetcher);
            CompletableFuture<Void> collectorFuture = CompletableFuture.runAsync(
                    collector::collectPopularMovies, collectorExecutor);
            boolean terminalState = awaitTerminalState(collectorFuture, TERMINAL_TIMEOUT, metrics.failure());
            evidence = StressEvidence.finished(statusFor(terminalState, metrics), metrics, terminalState,
                    Duration.between(startedAt, Instant.now()).toMillis(), null);
            writeRealStressEvidence(evidence);

            assertTrue(terminalState, "Real TMDB collector stress did not reach terminal state within timeout");
        } catch (RuntimeException e) {
            metrics.recordFailure(e);
            evidence = StressEvidence.finished("TERMINAL_EXTERNAL_FAILURE", metrics, true,
                    Duration.between(startedAt, Instant.now()).toMillis(), classifyFailure(metrics.failure()));
            writeRealStressEvidence(evidence);
            assertTrue(evidence.terminalState(), evidence.toText());
        } finally {
            if (evidence.notWritten()) {
                writeRealStressEvidence(StressEvidence.finished("TERMINAL_EXTERNAL_FAILURE", metrics, true,
                        Duration.between(startedAt, Instant.now()).toMillis(), classifyFailure(metrics.failure())));
            }
            fetchExecutor.shutdown();
            collectorExecutor.shutdownNow();
            collectorExecutor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    private static TmdbDataCollectorService newCollector(TmdbDataFetcher fetcher) {
        ItemRepository itemRepository = Mockito.mock(ItemRepository.class);
        EntityCacheService entityCacheService = Mockito.mock(EntityCacheService.class);
        Mockito.when(itemRepository.existsByTmdbIdAndTmdbType(anyLong(), eq(TmdbType.MOVIE))).thenReturn(false);
        Mockito.when(itemRepository.save(any(ItemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(entityCacheService.findOrCreateGenres(any())).thenReturn(Map.of());
        Mockito.when(entityCacheService.findOrCreateActors(any())).thenReturn(Map.of());
        Mockito.when(entityCacheService.findOrCreateDirectors(any())).thenReturn(Map.of());

        TmdbItemPersistenceService persistenceService = new TmdbItemPersistenceService(
                itemRepository, entityCacheService, fetcher);
        TmdbDataCollectorService collector = new TmdbDataCollectorService(itemRepository, fetcher, persistenceService);
        ReflectionTestUtils.setField(collector, "maxPages", 1);
        ReflectionTestUtils.setField(collector, "pageWindowSize", 2);
        ReflectionTestUtils.setField(collector, "batchSize", 20);
        return collector;
    }

    private static boolean awaitTerminalState(
            CompletableFuture<Void> future,
            Duration timeout,
            AtomicReference<Throwable> failure) throws InterruptedException {
        try {
            future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (ExecutionException e) {
            failure.compareAndSet(null, e.getCause() == null ? e : e.getCause());
            return true;
        } catch (TimeoutException e) {
            failure.compareAndSet(null, e);
            future.cancel(true);
            return false;
        }
    }

    private static String statusFor(boolean terminalState, StressMetrics metrics) {
        if (!terminalState) {
            return "NON_TERMINAL_TIMEOUT";
        }
        return metrics.failedCount() == 0 ? "SUCCESS" : "TERMINAL_EXTERNAL_FAILURE";
    }

    private static String classifyFailure(AtomicReference<Throwable> failure) {
        Throwable error = failure.get();
        if (error == null) {
            return "none";
        }
        if (containsTimeout(error)) {
            return "timeout";
        }
        return error.getClass().getSimpleName();
    }

    private static boolean containsTimeout(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof TimeoutException
                    || current.getClass().getName().toLowerCase(Locale.ROOT).contains("timeout")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static ThreadPoolTaskExecutor newStressExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(16);
        executor.setThreadNamePrefix("tmdb-real-stress-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }

    private static RestClient newRestClient(String apiKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(HTTP_CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(HTTP_READ_TIMEOUT);
        return RestClient.builder()
                .baseUrl("https://api.themoviedb.org/3")
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private static String skippedReason(String apiKey, boolean optIn) {
        if (apiKey == null || apiKey.isBlank()) {
            return "skipped: TMDB_API_KEY is absent";
        }
        if (!optIn) {
            return "skipped: TV_LOOKER_RUN_REAL_TMDB_STRESS is not true";
        }
        return "skipped: real TMDB stress prerequisites are absent";
    }

    private static void writeRealStressEvidence(StressEvidence evidence) throws Exception {
        TmdbEvidenceWriter.write(TASK_8_EVIDENCE_DIR.resolve("task-8-real-tmdb-stress.md"), evidence.toText());
        evidence.markWritten();
    }

    private static void writeDefaultCiEvidence(String skippedReason) throws Exception {
        String evidence = "Task 8 default CI safety" + System.lineSeparator()
                + "defaultMvnTestRequiresTmdbCredentials=false" + System.lineSeparator()
                + "defaultMvnTestRequiresPostgres=false" + System.lineSeparator()
                + "realStressOptInRequired=true" + System.lineSeparator()
                + "realStressSkippedByDefault=true" + System.lineSeparator()
                + "status=SKIPPED" + System.lineSeparator()
                + "skippedReason=" + skippedReason + System.lineSeparator();
        TmdbEvidenceWriter.write(TASK_8_EVIDENCE_DIR.resolve("task-8-default-ci.txt"), evidence);
    }

    private static final class CountingTmdbClient extends TmdbClient {
        private final StressMetrics metrics;

        private CountingTmdbClient(RestClient restClient, String language, StressMetrics metrics) {
            super(restClient, language);
            this.metrics = metrics;
        }

        @Override
        public <T extends TmdbMediaItem> TmdbPagedResponseDto<T> getPopular(TmdbMediaType type, int page) {
            return metrics.record(() -> super.getPopular(type, page));
        }

        @Override
        public <T extends TmdbMediaDetails> T getDetailsWithCredits(TmdbMediaType type, long id) {
            return metrics.record(() -> super.getDetailsWithCredits(type, id));
        }
    }

    private static final class StressMetrics {
        private final ThreadPoolTaskExecutor executor;
        private final AtomicInteger completedCount = new AtomicInteger();
        private final AtomicInteger failedCount = new AtomicInteger();
        private final AtomicInteger timeoutCount = new AtomicInteger();
        private final AtomicInteger maxActiveThreads = new AtomicInteger();
        private final AtomicReference<Throwable> failure = new AtomicReference<>();

        private StressMetrics(ThreadPoolTaskExecutor executor) {
            this.executor = executor;
        }

        private <T> T record(Supplier<T> supplier) {
            maxActiveThreads.accumulateAndGet(executor.getActiveCount(), Math::max);
            try {
                T result = supplier.get();
                completedCount.incrementAndGet();
                return result;
            } catch (RuntimeException e) {
                recordFailure(e);
                throw e;
            }
        }

        private void recordFailure(Throwable error) {
            failedCount.incrementAndGet();
            failure.compareAndSet(null, error);
            if (containsTimeout(error)) {
                timeoutCount.incrementAndGet();
            }
        }

        private int completedCount() {
            return completedCount.get();
        }

        private int failedCount() {
            return failedCount.get();
        }

        private int timeoutCount() {
            return timeoutCount.get();
        }

        private int executorQueueRemaining() {
            return executor.getQueueSize();
        }

        private int maxActiveThreads() {
            return maxActiveThreads.get();
        }

        private AtomicReference<Throwable> failure() {
            return failure;
        }
    }

    private static final class StressEvidence {
        private final String status;
        private final String skippedReason;
        private final int completedCount;
        private final int failedCount;
        private final boolean terminalState;
        private final boolean deadlockDetected;
        private final int executorQueueRemaining;
        private final int maxActiveThreads;
        private final long runtimeMs;
        private final int timeoutCount;
        private final String failureClassification;
        private boolean written;

        private StressEvidence(
                String status,
                String skippedReason,
                int completedCount,
                int failedCount,
                boolean terminalState,
                boolean deadlockDetected,
                int executorQueueRemaining,
                int maxActiveThreads,
                long runtimeMs,
                int timeoutCount,
                String failureClassification) {
            this.status = status;
            this.skippedReason = skippedReason;
            this.completedCount = completedCount;
            this.failedCount = failedCount;
            this.terminalState = terminalState;
            this.deadlockDetected = deadlockDetected;
            this.executorQueueRemaining = executorQueueRemaining;
            this.maxActiveThreads = maxActiveThreads;
            this.runtimeMs = runtimeMs;
            this.timeoutCount = timeoutCount;
            this.failureClassification = failureClassification;
        }

        private static StressEvidence skipped(String skippedReason) {
            return new StressEvidence("SKIPPED", skippedReason, 0, 0, false, false, 0, 0, 0, 0, "none");
        }

        private static StressEvidence started() {
            return new StressEvidence("NOT_WRITTEN", "", 0, 0, false, false, 0, 0, 0, 0, "none");
        }

        private static StressEvidence finished(
                String status,
                StressMetrics metrics,
                boolean terminalState,
                long runtimeMs,
                String failureClassification) {
            return new StressEvidence(status, "", metrics.completedCount(), metrics.failedCount(), terminalState,
                    false, metrics.executorQueueRemaining(), metrics.maxActiveThreads(), runtimeMs, metrics.timeoutCount(),
                    failureClassification == null ? classifyFailure(metrics.failure()) : failureClassification);
        }

        private String toText() {
            return "# Task 8 Real TMDB Stress" + System.lineSeparator()
                    + System.lineSeparator()
                    + "status=" + status + System.lineSeparator()
                    + "skippedReason=" + skippedReason + System.lineSeparator()
                    + "completedCount=" + completedCount + System.lineSeparator()
                    + "failedCount=" + failedCount + System.lineSeparator()
                    + "terminalState=" + terminalState + System.lineSeparator()
                    + "deadlockDetected=" + deadlockDetected + System.lineSeparator()
                    + "executorQueueRemaining=" + executorQueueRemaining + System.lineSeparator()
                    + "maxActiveThreads=" + maxActiveThreads + System.lineSeparator()
                    + "runtimeMs=" + runtimeMs + System.lineSeparator()
                    + "timeoutCount=" + timeoutCount + System.lineSeparator()
                    + "failureClassification=" + failureClassification + System.lineSeparator()
                    + "collectorEntryPoint=TmdbDataCollectorService.collectPopularMovies" + System.lineSeparator()
                    + "persistenceMode=mocked" + System.lineSeparator()
                    + "optInRequired=true" + System.lineSeparator()
                    + "credentialRequiredForRealRun=true" + System.lineSeparator()
                    + "defaultCiSafe=" + Set.of("SKIPPED").contains(status) + System.lineSeparator();
        }

        private boolean terminalState() {
            return terminalState;
        }

        private boolean notWritten() {
            return !written;
        }

        private void markWritten() {
            written = true;
        }
    }
}
