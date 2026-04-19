package org.tvl.tvlooker.api.dto.mapper;

import org.springframework.data.domain.Page;
import org.tvl.tvlooker.api.dto.response.PageResponse;

import java.util.List;

public class PageMapper {

    public static <T> PageResponse<T> toResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}