package org.tvl.tvlooker.service.tmdb.support;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class TmdbPhaseMetrics {
    private final AtomicInteger fetchStarts = new AtomicInteger();
    private final AtomicInteger fetchFinishes = new AtomicInteger();
    private final AtomicInteger persistenceStarts = new AtomicInteger();
    private final AtomicInteger persistenceFinishes = new AtomicInteger();
    private final AtomicReference<Instant> fetchStartedAt = new AtomicReference<>();
    private final AtomicReference<Instant> fetchFinishedAt = new AtomicReference<>();
    private final AtomicReference<Instant> persistenceStartedAt = new AtomicReference<>();
    private final AtomicReference<Instant> persistenceFinishedAt = new AtomicReference<>();

    public void markFetchStarted() {
        fetchStarts.incrementAndGet();
        fetchStartedAt.compareAndSet(null, Instant.now());
    }

    public void markFetchFinished() {
        fetchFinishes.incrementAndGet();
        fetchFinishedAt.set(Instant.now());
    }

    public void markPersistenceStarted() {
        persistenceStarts.incrementAndGet();
        persistenceStartedAt.compareAndSet(null, Instant.now());
    }

    public void markPersistenceFinished() {
        persistenceFinishes.incrementAndGet();
        persistenceFinishedAt.set(Instant.now());
    }

    public int fetchStarts() { return fetchStarts.get(); }
    public int fetchFinishes() { return fetchFinishes.get(); }
    public int persistenceStarts() { return persistenceStarts.get(); }
    public int persistenceFinishes() { return persistenceFinishes.get(); }

    public Duration fetchElapsed() { return elapsed(fetchStartedAt.get(), fetchFinishedAt.get()); }
    public Duration persistenceElapsed() { return elapsed(persistenceStartedAt.get(), persistenceFinishedAt.get()); }

    private static Duration elapsed(Instant start, Instant end) {
        if (start == null || end == null || end.isBefore(start)) {
            return Duration.ZERO;
        }
        return Duration.between(start, end);
    }
}