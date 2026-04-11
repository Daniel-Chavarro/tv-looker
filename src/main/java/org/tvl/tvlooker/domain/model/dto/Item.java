package org.tvl.tvlooker.domain.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.tvl.tvlooker.domain.model.enums.TmdbType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Builder
@Getter
@AllArgsConstructor
public class Item {
    private final Long id;
    private final String title;
    private final String overview;
    private final LocalDate releaseDate;
    private final BigDecimal popularity;
    private final BigDecimal voteAverage;
    private final TmdbType tmdbType;
    private final Long tmdbId;
    private final Set<Genre> genres;
    private final Set<Director> directors;
    private final Set<ActorItem> actorsInItem;
}
