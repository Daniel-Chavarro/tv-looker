package org.tvl.tvlooker.domain.motor.utils;

import java.util.List;

public record DataPage<T>(List<T> content, boolean hasNext) {

    public DataPage {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }

    public static <T> DataPage<T> empty() {
        return new DataPage<>(List.of(), false);
    }
}
