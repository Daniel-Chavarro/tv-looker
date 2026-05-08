package org.tvl.tvlooker.service.tmdb;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import org.tvl.tvlooker.persistence.tmdb.TmdbClient;
import org.tvl.tvlooker.persistence.tmdb.TmdbMediaType;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.service.tmdb.support.TmdbAsyncTestSupport;
import org.tvl.tvlooker.service.tmdb.support.TmdbEvidenceWriter;
import org.tvl.tvlooker.service.tmdb.support.TmdbExecutorSnapshot;
import org.tvl.tvlooker.service.tmdb.support.TmdbFakeDtoFactory;
import org.tvl.tvlooker.service.tmdb.support.TmdbPhaseMetrics;
import org.tvl.tvlooker.service.tmdb.support.TmdbThreadDumpSnapshot;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TMDB collector executor starvation reproduction")
class TmdbCollectorExecutorStarvationTest {
    private static final Path EVIDENCE_DIR = Path.of(
            ".sisyphus/evidence/collector-thread-blocking/task-2");
    private static final int SCALED_EQUIVALENT_PAGE_COUNT = 500;
    private static final int TMDB_ITEMS_PER_PAGE = 20;
    private static final int MODELLED_ITEM_COUNT = SCALED_EQUIVALENT_PAGE_COUNT * TMDB_ITEMS_PER_PAGE;
    private static final Duration BOUNDED_WAIT = Duration.ofMillis(750);

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private TmdbItemPersistenceService persistenceService;

    @Mock
    private TmdbClient tmdbClient;

    @Mock
    private TmdbDataFetcher mockedDataFetcher;

    private ThreadPoolTaskExecutor boundedTmdbExecutor;
    private ExecutorService parentProbeExecutor;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (boundedTmdbExecutor != null) {
            boundedTmdbExecutor.shutdown();
        }
        if (parentProbeExecutor != null) {
            parentProbeExecutor.shutdownNow();
            parentProbeExecutor.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    @Test
    @Timeout(5)
    @DisplayName("shared TMDB executor can starve child fetches when parent collectors occupy every worker")
    void collectPopularMoviesWithSharedBoundedExecutorShouldReachTerminalState() throws Exception {
        boundedTmdbExecutor = newBoundedTmdbExecutor(2, 32, "tmdb-starvation-");
        TmdbDataFetcher realFetcher = new TmdbDataFetcher(tmdbClient, boundedTmdbExecutor, 35.0);
        TmdbDataCollectorService collectorService = newCollector(realFetcher);
        TmdbPhaseMetrics phases = new TmdbPhaseMetrics();

        when(tmdbClient.getPopular(TmdbMediaType.MOVIE, 1)).thenAnswer(invocation -> {
            phases.markFetchStarted();
            phases.markFetchFinished();
            return moviePage(1);
        });
        when(persistenceService.discoverAndPersistNewMovies(anyList())).thenAnswer(invocation -> {
            phases.markPersistenceStarted();
            phases.markPersistenceFinished();
            return invocation.getArgument(0, List.class).size();
        });

        Instant startedAt = Instant.now();
        CompletableFuture<Void> firstParent = CompletableFuture.runAsync(
                collectorService::collectPopularMovies, boundedTmdbExecutor);
        CompletableFuture<Void> secondParent = CompletableFuture.runAsync(
                collectorService::collectPopularMovies, boundedTmdbExecutor);
        CompletableFuture<Void> bothParents = CompletableFuture.allOf(firstParent, secondParent);

        boolean terminalState = awaitTerminalState(bothParents, BOUNDED_WAIT);
        String evidence = starvationEvidence(startedAt, terminalState, phases);
        TmdbEvidenceWriter.write(EVIDENCE_DIR.resolve("task-2-prefix-reproduction.txt"), evidence);
        writeWorkloadModel();

        assertTrue(terminalState, evidence);
    }

    @Test
    @Timeout(5)
    @DisplayName("never-completing TMDB fetch keeps collector non-terminal without credentials or network")
    void collectPopularMoviesWithNeverCompletingFetchShouldReachTerminalState() throws Exception {
        TmdbDataCollectorService collectorService = newCollector(mockedDataFetcher);
        CompletableFuture<TmdbPagedResponseDto<TmdbMovieDto>> stuckFetch = TmdbAsyncTestSupport.neverCompleting();
        when(mockedDataFetcher.fetchPopularMoviesAsync(1)).thenReturn(stuckFetch);

        parentProbeExecutor = Executors.newSingleThreadExecutor(daemonThreadFactory("tmdb-stuck-probe-"));
        Instant startedAt = Instant.now();
        CompletableFuture<Void> collectorFuture = CompletableFuture.runAsync(
                collectorService::collectPopularMovies, parentProbeExecutor);

        boolean terminalState = awaitTerminalState(collectorFuture, BOUNDED_WAIT);
        String evidence = stuckFutureEvidence(startedAt, terminalState, stuckFetch);
        TmdbEvidenceWriter.write(EVIDENCE_DIR.resolve("task-2-no-real-tmdb.txt"), evidence);

        assertTrue(terminalState, evidence);
    }

    private TmdbDataCollectorService newCollector(TmdbDataFetcher dataFetcher) {
        TmdbDataCollectorService collectorService = new TmdbDataCollectorService(
                itemRepository, dataFetcher, persistenceService);
        ReflectionTestUtils.setField(collectorService, "maxPages", SCALED_EQUIVALENT_PAGE_COUNT);
        ReflectionTestUtils.setField(collectorService, "batchSize", 50);
        return collectorService;
    }

    private static ThreadPoolTaskExecutor newBoundedTmdbExecutor(int poolSize, int queueCapacity, String prefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(prefix);
        executor.setDaemon(true);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }

    private static TmdbPagedResponseDto<TmdbMovieDto> moviePage(int page) {
        return TmdbFakeDtoFactory.page(
                page,
                List.of(TmdbFakeDtoFactory.movie(page)),
                SCALED_EQUIVALENT_PAGE_COUNT,
                MODELLED_ITEM_COUNT);
    }

    private static boolean awaitTerminalState(CompletableFuture<Void> future, Duration timeout) throws Exception {
        try {
            future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (TimeoutException e) {
            future.cancel(true);
            return false;
        }
    }

    private String starvationEvidence(Instant startedAt, boolean terminalState, TmdbPhaseMetrics phases) {
        TmdbExecutorSnapshot executorSnapshot = TmdbExecutorSnapshot.capture(boundedTmdbExecutor);
        TmdbThreadDumpSnapshot threadDumpSnapshot = TmdbThreadDumpSnapshot.capture();
        return "Task 2 prefix reproduction: parent/child executor starvation" + System.lineSeparator()
                + "terminalState=" + terminalState + System.lineSeparator()
                + "boundedWaitMillis=" + BOUNDED_WAIT.toMillis() + System.lineSeparator()
                + "elapsedMillis=" + Duration.between(startedAt, Instant.now()).toMillis() + System.lineSeparator()
                + "executor=" + executorSnapshot + System.lineSeparator()
                + "phase.fetchStarts=" + phases.fetchStarts() + System.lineSeparator()
                + "phase.fetchFinishes=" + phases.fetchFinishes() + System.lineSeparator()
                + "phase.persistenceStarts=" + phases.persistenceStarts() + System.lineSeparator()
                + "phase.persistenceFinishes=" + phases.persistenceFinishes() + System.lineSeparator()
                + "threadDumpCounts=" + threadDumpSnapshot.counts() + System.lineSeparator()
                + "modelledPages=" + SCALED_EQUIVALENT_PAGE_COUNT + System.lineSeparator()
                + "modelledItems=" + MODELLED_ITEM_COUNT + System.lineSeparator()
                + "networkAccess=false" + System.lineSeparator()
                + "credentialRequired=false" + System.lineSeparator();
    }

    private static String stuckFutureEvidence(
            Instant startedAt,
            boolean terminalState,
            CompletableFuture<TmdbPagedResponseDto<TmdbMovieDto>> stuckFetch) {
        TmdbThreadDumpSnapshot threadDumpSnapshot = TmdbThreadDumpSnapshot.capture();
        return "Task 2 stuck fetch reproduction: never-completing mocked TMDB future" + System.lineSeparator()
                + "terminalState=" + terminalState + System.lineSeparator()
                + "boundedWaitMillis=" + BOUNDED_WAIT.toMillis() + System.lineSeparator()
                + "elapsedMillis=" + Duration.between(startedAt, Instant.now()).toMillis() + System.lineSeparator()
                + "stuckFetchDone=" + stuckFetch.isDone() + System.lineSeparator()
                + "threadDumpCounts=" + threadDumpSnapshot.counts() + System.lineSeparator()
                + "tmdbApiKeyRead=false" + System.lineSeparator()
                + "networkAccess=false" + System.lineSeparator()
                + "credentialRequired=false" + System.lineSeparator();
    }

    private static void writeWorkloadModel() throws IOException {
        String content = "# Task 2 Workload Model" + System.lineSeparator()
                + System.lineSeparator()
                + "This reproduction uses a scaled-equivalent fake TMDB workload." + System.lineSeparator()
                + System.lineSeparator()
                + "- TMDB catalogue page size assumption: " + TMDB_ITEMS_PER_PAGE + " items/page" + System.lineSeparator()
                + "- Configured fake page count: " + SCALED_EQUIVALENT_PAGE_COUNT + " pages" + System.lineSeparator()
                + "- Modelled workload: " + SCALED_EQUIVALENT_PAGE_COUNT + " * " + TMDB_ITEMS_PER_PAGE
                + " = " + MODELLED_ITEM_COUNT + " items" + System.lineSeparator()
                + "- Executor shape: 2 workers with a bounded queue, shared by parent collector tasks and child TMDB fetch tasks"
                + System.lineSeparator()
                + "- Determinism: both workers are occupied by parent collectors before their first child fetches can run,"
                + " so the test reaches a bounded non-terminal state without real TMDB access."
                + System.lineSeparator();
        TmdbEvidenceWriter.write(EVIDENCE_DIR.resolve("task-2-workload-model.md"), content);
    }

    private static ThreadFactory daemonThreadFactory(String prefix) {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
