package org.tvl.tvlooker.service.tmdb.support;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record TmdbThreadDumpSnapshot(List<ThreadDumpEntry> threads, Map<TmdbBlockingCategory, Long> counts) {
    public static TmdbThreadDumpSnapshot capture() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] infos = threadMXBean.dumpAllThreads(true, true);
        List<ThreadDumpEntry> entries = new ArrayList<>(infos.length);
        Map<TmdbBlockingCategory, Long> counts = new EnumMap<>(TmdbBlockingCategory.class);
        for (TmdbBlockingCategory category : TmdbBlockingCategory.values()) {
            counts.put(category, 0L);
        }
        for (ThreadInfo info : infos) {
            TmdbBlockingCategory category = classify(info);
            counts.put(category, counts.get(category) + 1);
            entries.add(new ThreadDumpEntry(
                    info.getThreadId(),
                    info.getThreadName(),
                    info.getThreadState().name(),
                    category,
                    info.toString()));
        }
        return new TmdbThreadDumpSnapshot(List.copyOf(entries), Map.copyOf(counts));
    }

    public long count(TmdbBlockingCategory category) {
        return counts.getOrDefault(category, 0L);
    }

    public record ThreadDumpEntry(long id, String name, String state, TmdbBlockingCategory category, String raw) {}

    private static TmdbBlockingCategory classify(ThreadInfo info) {
        String text = info.toString().toLowerCase(Locale.ROOT);
        for (StackTraceElement element : info.getStackTrace()) {
            String className = element.getClassName().toLowerCase(Locale.ROOT);
            String methodName = element.getMethodName().toLowerCase(Locale.ROOT);
            if (className.contains("completablefuture") || className.contains("forkjoin") || methodName.contains("join")) {
                return TmdbBlockingCategory.COMPLETABLE_FUTURE;
            }
            if (className.contains("restclient") || className.contains("httpclient") || className.contains("webclient")) {
                return TmdbBlockingCategory.REST_CLIENT;
            }
            if (className.contains("ratelimiter")) {
                return TmdbBlockingCategory.RATE_LIMITER;
            }
            if (className.contains("hibernate") || className.contains("jdbc") || className.contains("datasource")
                    || className.contains("hikari") || className.contains("preparedstatement") || className.contains("connection")) {
                return TmdbBlockingCategory.JDBC_HIBERNATE;
            }
        }
        if (text.contains("completablefuture") || text.contains("join")) {
            return TmdbBlockingCategory.COMPLETABLE_FUTURE;
        }
        return TmdbBlockingCategory.OTHER;
    }
}