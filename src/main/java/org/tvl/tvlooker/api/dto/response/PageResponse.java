package org.tvl.tvlooker.api.dto.response;

import java.util.List;

/**
 * DTO for paginated responses.
 *
 * @param <T> the type of content in the page
 */
public record PageResponse<T>(
    List<T> content,
    long totalItems,
    int actualPage,
    int totalPages,
    boolean isLast
) {}