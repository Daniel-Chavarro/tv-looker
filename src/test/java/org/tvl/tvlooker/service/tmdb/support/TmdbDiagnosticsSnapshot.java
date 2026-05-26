package org.tvl.tvlooker.service.tmdb.support;

import java.time.Duration;

public record TmdbDiagnosticsSnapshot(
        TmdbExecutorSnapshot executor,
        TmdbThreadDumpSnapshot threadDump,
        TmdbPhaseMetrics phases,
        Duration elapsed
) {}