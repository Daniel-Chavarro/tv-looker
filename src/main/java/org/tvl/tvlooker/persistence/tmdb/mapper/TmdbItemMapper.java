package org.tvl.tvlooker.persistence.tmdb.mapper;

import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Maps TMDB movie/TV DTOs to Item JPA entities.
 * Does NOT set genres, actors, or directors — those are handled separately.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-10
 */
public final class TmdbItemMapper {

    private TmdbItemMapper() {
        // Utility class
    }

    /**
     * Creates a new Item entity from a TMDB movie DTO.
     */
    public static Item fromMovie(TmdbMovieDto dto) {
        return Item.builder()
                .tmdbId(dto.id())
                .tmdbType(TmdbType.MOVIE)
                .title(dto.title())
                .overview(dto.overview())
                .releaseDate(parseDate(dto.releaseDate()))
                .popularity(BigDecimal.valueOf(dto.popularity()))
                .voteAverage(BigDecimal.valueOf(dto.voteAverage()))
                .build();
    }

    /**
     * Creates a new Item entity from a TMDB TV show DTO.
     */
    public static Item fromTvShow(TmdbTvShowDto dto) {
        return Item.builder()
                .tmdbId(dto.id())
                .tmdbType(TmdbType.TV)
                .title(dto.name())
                .overview(dto.overview())
                .releaseDate(parseDate(dto.firstAirDate()))
                .popularity(BigDecimal.valueOf(dto.popularity()))
                .voteAverage(BigDecimal.valueOf(dto.voteAverage()))
                .build();
    }

    /**
     * Updates mutable fields on an existing Item with fresh TMDB movie data.
     */
    public static void updateFromMovie(Item existing, TmdbMovieDto dto) {
        existing.setTitle(dto.title());
        existing.setOverview(dto.overview());
        existing.setPopularity(BigDecimal.valueOf(dto.popularity()));
        existing.setVoteAverage(BigDecimal.valueOf(dto.voteAverage()));
        existing.setReleaseDate(parseDate(dto.releaseDate()));
    }

    /**
     * Updates mutable fields on an existing Item with fresh TMDB TV show data.
     */
    public static void updateFromTvShow(Item existing, TmdbTvShowDto dto) {
        existing.setTitle(dto.name());
        existing.setOverview(dto.overview());
        existing.setPopularity(BigDecimal.valueOf(dto.popularity()));
        existing.setVoteAverage(BigDecimal.valueOf(dto.voteAverage()));
        existing.setReleaseDate(parseDate(dto.firstAirDate()));
    }

    private static LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

