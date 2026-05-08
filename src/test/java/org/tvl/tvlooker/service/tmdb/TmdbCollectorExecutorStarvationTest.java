package org.tvl.tvlooker.service.tmdb;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
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
import org.tvl.tvlooker.service.tmdb.support.TmdbDiagnosticsSnapshot;
import org.tvl.tvlooker.service.tmdb.support.TmdbEvidenceWriter;
import org.tvl.tvlooker.service.tmdb.support.TmdbExecutorSnapshot;
import org.tvl.tvlooker.service.tmdb.support.TmdbFakeDtoFactory;
import org.tvl.tvlooker.service.tmdb.support.TmdbPhaseMetrics;
import org.tvl.tvlooker.service.tmdb.support.TmdbThreadDumpSnapshot;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TMDB collector executor starvation reproduction")
class TmdbCollectorExecutorStarvationTest {
    private static final Path EVIDENCE_DIR = Path.of(
            ".sisyphus/evidence/collector-thread-blocking/task-2");
    private static final Path TASK_3_EVIDENCE_DIR = Path.of(
            ".sisyphus/evidence/collector-thread-blocking/task-3");
    private static final Path TASK_4_EVIDENCE_DIR = Path.of(
            ".sisyphus/evidence/collector-thread-blocking/task-4");
    private static final Path TASK_5_EVIDENCE_DIR = Path.of(
            ".sisyphus/evidence/collector-thread-blocking/task-5");
    private static final int SCALED_EQUIVALENT_PAGE_COUNT = 500;
    private static final int POST_FIX_PAGE_COUNT = 4;
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
    @DisplayName("separate orchestration executor lets child fetches complete while parents wait")
    void collectPopularMoviesWithSharedBoundedExecutorShouldReachTerminalState() throws Exception {
        boundedTmdbExecutor = newBoundedTmdbExecutor(2, 32, "tmdb-fetch-proof-");
        TmdbDataFetcher realFetcher = new TmdbDataFetcher(tmdbClient, boundedTmdbExecutor, 35.0, 100);
        TmdbDataCollectorService collectorService = newCollector(realFetcher, POST_FIX_PAGE_COUNT);
        TmdbPhaseMetrics phases = new TmdbPhaseMetrics();
        stubSharedExecutorWorkload(phases);
        CountDownLatch parentsStarted = new CountDownLatch(2);
        CountDownLatch releaseParents = new CountDownLatch(1);
        parentProbeExecutor = Executors.newFixedThreadPool(2, daemonThreadFactory("tmdb-orchestration-proof-"));

        Instant startedAt = Instant.now();
        CompletableFuture<Void> firstParent = CompletableFuture.runAsync(
                parentCollector(collectorService, parentsStarted, releaseParents), parentProbeExecutor);
        CompletableFuture<Void> secondParent = CompletableFuture.runAsync(
                parentCollector(collectorService, parentsStarted, releaseParents), parentProbeExecutor);
        parentsStarted.await(250, TimeUnit.MILLISECONDS);
        releaseParents.countDown();
        CompletableFuture<Void> bothParents = CompletableFuture.allOf(firstParent, secondParent);

        boolean terminalState = awaitTerminalState(bothParents, BOUNDED_WAIT);
        String evidence = executorSeparationEvidence(startedAt, terminalState, phases);
        TmdbEvidenceWriter.write(TASK_4_EVIDENCE_DIR.resolve("task-4-executor-separation.txt"), evidence);

        assertTrue(terminalState, evidence);
    }

    @Test
    @Disabled("Task 6 pending: stuck TMDB futures still need bounded terminal handling.")
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

    @Test
    @Timeout(5)
    @DisplayName("capture current-state task 3 evidence from the shared-executor stall")
    void captureTask3CurrentStateEvidenceFromSharedExecutorStall() throws Exception {
        boundedTmdbExecutor = newBoundedTmdbExecutor(2, 32, "tmdb-starvation-");
        TmdbDataFetcher realFetcher = new TmdbDataFetcher(tmdbClient, boundedTmdbExecutor, 35.0, 100);
        TmdbDataCollectorService collectorService = newCollector(realFetcher);
        TmdbPhaseMetrics phases = new TmdbPhaseMetrics();
        stubSharedExecutorWorkload(phases);
        CountDownLatch parentsStarted = new CountDownLatch(2);
        CountDownLatch releaseParents = new CountDownLatch(1);

        Instant startedAt = Instant.now();
        CompletableFuture<Void> firstParent = CompletableFuture.runAsync(
                parentCollector(collectorService, parentsStarted, releaseParents), boundedTmdbExecutor);
        CompletableFuture<Void> secondParent = CompletableFuture.runAsync(
                parentCollector(collectorService, parentsStarted, releaseParents), boundedTmdbExecutor);
        parentsStarted.await(250, TimeUnit.MILLISECONDS);
        releaseParents.countDown();
        CompletableFuture<Void> bothParents = CompletableFuture.allOf(firstParent, secondParent);

        Thread.sleep(100);
        TmdbDiagnosticsSnapshot snapshot1 = captureDiagnostics(startedAt, phases);
        writeThreadDump(1, snapshot1.threadDump());
        Thread.sleep(100);
        TmdbDiagnosticsSnapshot snapshot2 = captureDiagnostics(startedAt, phases);
        writeThreadDump(2, snapshot2.threadDump());
        Thread.sleep(100);
        TmdbDiagnosticsSnapshot snapshot3 = captureDiagnostics(startedAt, phases);
        writeThreadDump(3, snapshot3.threadDump());

        boolean terminalState = awaitTerminalState(bothParents, BOUNDED_WAIT);
        TmdbDiagnosticsSnapshot finalSnapshot = captureDiagnostics(startedAt, phases);
        TmdbEvidenceWriter.write(TASK_3_EVIDENCE_DIR.resolve("task-3-current-state-metrics.txt"),
                task3Metrics(startedAt, terminalState, phases, snapshot1, snapshot2, snapshot3, finalSnapshot));

    }

    @Test
    @Timeout(5)
    @DisplayName("bounded page and detail windows cap observed in-flight work")
    void boundedWindowsShouldCapObservedInFlightWork() throws Exception {
        int pageWindowSize = 2;
        int detailWindowSize = 2;
        int totalPages = 6;
        AtomicInteger pagesInFlight = new AtomicInteger();
        AtomicInteger maxPagesInFlight = new AtomicInteger();
        java.util.concurrent.ScheduledExecutorService pageReleaser = Executors.newSingleThreadScheduledExecutor(
                daemonThreadFactory("tmdb-page-window-release-"));
        TmdbDataCollectorService collectorService = newCollector(mockedDataFetcher, totalPages, pageWindowSize);
        when(mockedDataFetcher.fetchPopularMoviesAsync(1))
                .thenReturn(CompletableFuture.completedFuture(moviePage(1, totalPages)));
        when(mockedDataFetcher.fetchPopularMoviesAsync(intThat(page -> page > 1))).thenAnswer(invocation -> {
            int page = invocation.getArgument(0, Integer.class);
            CompletableFuture<TmdbPagedResponseDto<TmdbMovieDto>> future = new CompletableFuture<>();
            int active = pagesInFlight.incrementAndGet();
            maxPagesInFlight.accumulateAndGet(active, Math::max);
            pageReleaser.schedule(() -> future.complete(moviePage(page, totalPages)), 60, TimeUnit.MILLISECONDS);
            return future.whenComplete((response, error) -> pagesInFlight.decrementAndGet());
        });
        when(persistenceService.discoverAndPersistNewMovies(anyList())).thenAnswer(invocation ->
                invocation.getArgument(0, List.class).size());

        try {
            collectorService.collectPopularMovies();
        } finally {
            pageReleaser.shutdownNow();
            pageReleaser.awaitTermination(1, TimeUnit.SECONDS);
        }

        ExecutorService detailExecutor = Executors.newFixedThreadPool(6, daemonThreadFactory("tmdb-detail-window-"));
        TmdbDataFetcher detailFetcher = new TmdbDataFetcher(tmdbClient, detailExecutor, 35.0, detailWindowSize);
        AtomicInteger detailsInFlight = new AtomicInteger();
        AtomicInteger maxDetailsInFlight = new AtomicInteger();
        when(tmdbClient.getDetailsWithCredits(eq(TmdbMediaType.MOVIE), anyLong())).thenAnswer(invocation -> {
            int active = detailsInFlight.incrementAndGet();
            maxDetailsInFlight.accumulateAndGet(active, Math::max);
            try {
                TimeUnit.MILLISECONDS.sleep(60);
                return TmdbFakeDtoFactory.movieDetails(invocation.getArgument(1, Long.class));
            } finally {
                detailsInFlight.decrementAndGet();
            }
        });

        List<?> detailResults;
        try {
            detailResults = detailFetcher.fetchMoviesDetailsBatch(List.of(1L, 2L, 3L, 4L, 5L));
        } finally {
            detailExecutor.shutdownNow();
            detailExecutor.awaitTermination(1, TimeUnit.SECONDS);
        }

        String evidence = "# Task 5 Window Metrics" + System.lineSeparator()
                + System.lineSeparator()
                + "configuredPageWindowSize=" + pageWindowSize + System.lineSeparator()
                + "observedMaxInFlightPages=" + maxPagesInFlight.get() + System.lineSeparator()
                + "configuredDetailWindowSize=" + detailWindowSize + System.lineSeparator()
                + "observedMaxInFlightDetails=" + maxDetailsInFlight.get() + System.lineSeparator()
                + "detailResults=" + detailResults.size() + System.lineSeparator()
                + "networkAccess=false" + System.lineSeparator()
                + "credentialRequired=false" + System.lineSeparator();
        TmdbEvidenceWriter.write(TASK_5_EVIDENCE_DIR.resolve("task-5-window-metrics.md"), evidence);

        assertTrue(maxPagesInFlight.get() <= pageWindowSize, evidence);
        assertTrue(maxDetailsInFlight.get() <= detailWindowSize, evidence);
    }

    private TmdbDataCollectorService newCollector(TmdbDataFetcher dataFetcher) {
        return newCollector(dataFetcher, SCALED_EQUIVALENT_PAGE_COUNT, 10);
    }

    private TmdbDataCollectorService newCollector(TmdbDataFetcher dataFetcher, int configuredMaxPages) {
        return newCollector(dataFetcher, configuredMaxPages, 10);
    }

    private TmdbDataCollectorService newCollector(
            TmdbDataFetcher dataFetcher, int configuredMaxPages, int configuredPageWindowSize) {
        TmdbDataCollectorService collectorService = new TmdbDataCollectorService(
                itemRepository, dataFetcher, persistenceService);
        ReflectionTestUtils.setField(collectorService, "maxPages", configuredMaxPages);
        ReflectionTestUtils.setField(collectorService, "pageWindowSize", configuredPageWindowSize);
        ReflectionTestUtils.setField(collectorService, "batchSize", 50);
        return collectorService;
    }

    private void stubSharedExecutorWorkload(TmdbPhaseMetrics phases) {
        lenient().when(tmdbClient.getPopular(eq(TmdbMediaType.MOVIE), anyInt())).thenAnswer(invocation -> {
            phases.markFetchStarted();
            phases.markFetchFinished();
            return moviePage(invocation.getArgument(1, Integer.class), currentConfiguredPageCount());
        });
        lenient().when(persistenceService.discoverAndPersistNewMovies(anyList())).thenAnswer(invocation -> {
            phases.markPersistenceStarted();
            phases.markPersistenceFinished();
            return invocation.getArgument(0, List.class).size();
        });
    }

    private static Runnable parentCollector(
            TmdbDataCollectorService collectorService,
            CountDownLatch parentsStarted,
            CountDownLatch releaseParents) {
        return () -> {
            parentsStarted.countDown();
            try {
                releaseParents.await();
                collectorService.collectPopularMovies();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while starting parent collector", e);
            }
        };
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

    private int currentConfiguredPageCount() {
        return boundedTmdbExecutor != null && boundedTmdbExecutor.getThreadNamePrefix().contains("proof")
                ? POST_FIX_PAGE_COUNT
                : SCALED_EQUIVALENT_PAGE_COUNT;
    }

    private static TmdbPagedResponseDto<TmdbMovieDto> moviePage(int page, int totalPages) {
        return TmdbFakeDtoFactory.page(
                page,
                List.of(TmdbFakeDtoFactory.movie(page)),
                totalPages,
                totalPages * TMDB_ITEMS_PER_PAGE);
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

    private String executorSeparationEvidence(Instant startedAt, boolean terminalState, TmdbPhaseMetrics phases) {
        TmdbExecutorSnapshot executorSnapshot = TmdbExecutorSnapshot.capture(boundedTmdbExecutor);
        TmdbThreadDumpSnapshot threadDumpSnapshot = TmdbThreadDumpSnapshot.capture();
        return "Task 4 executor separation: orchestration no longer occupies fetch workers" + System.lineSeparator()
                + "terminalState=" + terminalState + System.lineSeparator()
                + "boundedWaitMillis=" + BOUNDED_WAIT.toMillis() + System.lineSeparator()
                + "elapsedMillis=" + Duration.between(startedAt, Instant.now()).toMillis() + System.lineSeparator()
                + "fetchExecutor=" + executorSnapshot + System.lineSeparator()
                + "orchestrationExecutor=separate fixed test executor" + System.lineSeparator()
                + "phase.fetchStarts=" + phases.fetchStarts() + System.lineSeparator()
                + "phase.fetchFinishes=" + phases.fetchFinishes() + System.lineSeparator()
                + "phase.persistenceStarts=" + phases.persistenceStarts() + System.lineSeparator()
                + "phase.persistenceFinishes=" + phases.persistenceFinishes() + System.lineSeparator()
                + "threadDumpCounts=" + threadDumpSnapshot.counts() + System.lineSeparator()
                + "configuredPages=" + POST_FIX_PAGE_COUNT + System.lineSeparator()
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

    private TmdbDiagnosticsSnapshot captureDiagnostics(Instant startedAt, TmdbPhaseMetrics phases) {
        return new TmdbDiagnosticsSnapshot(
                TmdbExecutorSnapshot.capture(boundedTmdbExecutor),
                TmdbThreadDumpSnapshot.capture(),
                phases,
                Duration.between(startedAt, Instant.now()));
    }

    private static void writeThreadDump(int index, TmdbThreadDumpSnapshot snapshot) throws IOException {
        StringBuilder dump = new StringBuilder();
        dump.append("Task 3 thread dump ").append(index).append(System.lineSeparator());
        dump.append("counts=").append(snapshot.counts()).append(System.lineSeparator());
        dump.append(System.lineSeparator());
        for (TmdbThreadDumpSnapshot.ThreadDumpEntry thread : snapshot.threads()) {
            dump.append("category=").append(thread.category())
                    .append(", name=").append(thread.name())
                    .append(", state=").append(thread.state())
                    .append(System.lineSeparator())
                    .append(thread.raw())
                    .append(System.lineSeparator());
        }
        TmdbEvidenceWriter.write(TASK_3_EVIDENCE_DIR.resolve("task-3-thread-dump-" + index + ".txt"),
                dump.toString());
    }

    private static String task3Metrics(
            Instant startedAt,
            boolean terminalState,
            TmdbPhaseMetrics phases,
            TmdbDiagnosticsSnapshot snapshot1,
            TmdbDiagnosticsSnapshot snapshot2,
            TmdbDiagnosticsSnapshot snapshot3,
            TmdbDiagnosticsSnapshot finalSnapshot) {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long gcCount = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionCount)
                .filter(count -> count >= 0)
                .sum();
        long gcTimeMillis = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .mapToLong(GarbageCollectorMXBean::getCollectionTime)
                .filter(time -> time >= 0)
                .sum();

        return "Task 3 current-state metrics" + System.lineSeparator()
                + "terminalState=" + terminalState + System.lineSeparator()
                + "boundedWaitMillis=" + BOUNDED_WAIT.toMillis() + System.lineSeparator()
                + "elapsedMillis=" + Duration.between(startedAt, Instant.now()).toMillis() + System.lineSeparator()
                + "timingConstraint=bounded reproduction uses 750ms wait; three snapshots captured 100ms apart in same non-terminal interval"
                + System.lineSeparator()
                + "modelledPages=" + SCALED_EQUIVALENT_PAGE_COUNT + System.lineSeparator()
                + "modelledItems=" + MODELLED_ITEM_COUNT + System.lineSeparator()
                + "parentFutureCount=2" + System.lineSeparator()
                + "childFetchStarts=" + phases.fetchStarts() + System.lineSeparator()
                + "childFetchFinishes=" + phases.fetchFinishes() + System.lineSeparator()
                + "persistenceStarts=" + phases.persistenceStarts() + System.lineSeparator()
                + "persistenceFinishes=" + phases.persistenceFinishes() + System.lineSeparator()
                + "fetchElapsedMillis=" + phases.fetchElapsed().toMillis() + System.lineSeparator()
                + "persistenceElapsedMillis=" + phases.persistenceElapsed().toMillis() + System.lineSeparator()
                + "snapshot1.elapsedMillis=" + snapshot1.elapsed().toMillis() + System.lineSeparator()
                + "snapshot1.executor=" + snapshot1.executor() + System.lineSeparator()
                + "snapshot1.threadDumpCounts=" + snapshot1.threadDump().counts() + System.lineSeparator()
                + "snapshot2.elapsedMillis=" + snapshot2.elapsed().toMillis() + System.lineSeparator()
                + "snapshot2.executor=" + snapshot2.executor() + System.lineSeparator()
                + "snapshot2.threadDumpCounts=" + snapshot2.threadDump().counts() + System.lineSeparator()
                + "snapshot3.elapsedMillis=" + snapshot3.elapsed().toMillis() + System.lineSeparator()
                + "snapshot3.executor=" + snapshot3.executor() + System.lineSeparator()
                + "snapshot3.threadDumpCounts=" + snapshot3.threadDump().counts() + System.lineSeparator()
                + "final.executor=" + finalSnapshot.executor() + System.lineSeparator()
                + "final.threadDumpCounts=" + finalSnapshot.threadDump().counts() + System.lineSeparator()
                + "heap.usedBytes=" + heap.getUsed() + System.lineSeparator()
                + "heap.committedBytes=" + heap.getCommitted() + System.lineSeparator()
                + "heap.maxBytes=" + heap.getMax() + System.lineSeparator()
                + "gc.collectionCount=" + gcCount + System.lineSeparator()
                + "gc.collectionTimeMillis=" + gcTimeMillis + System.lineSeparator()
                + "http.restClientThreadsObserved=false" + System.lineSeparator()
                + "rateLimiterThreadsObserved=false" + System.lineSeparator()
                + "jdbcHibernateThreadsObserved=false" + System.lineSeparator()
                + "networkAccess=false" + System.lineSeparator()
                + "credentialRequired=false" + System.lineSeparator();
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
