package org.tvl.tvlooker.service.tmdb.support;

import org.tvl.tvlooker.persistence.tmdb.dto.TmdbChangesDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreListDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbPagedResponseDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;

import java.util.List;

public final class TmdbFakeDtoFactory {
    private TmdbFakeDtoFactory() {}

    public static TmdbGenreDto genre(long id, String name) {
        return new TmdbGenreDto((int) id, name);
    }

    public static TmdbGenreListDto genreList(TmdbGenreDto... genres) {
        return new TmdbGenreListDto(List.of(genres));
    }

    public static TmdbCreditsDto credits() {
        return new TmdbCreditsDto(1L, List.of(), List.of());
    }

    public static TmdbMovieDto movie(long id) {
        return movie(id, "Test Movie " + id);
    }

    public static TmdbMovieDto movie(long id, String title) {
        return new TmdbMovieDto(id, title, "Overview", "2026-04-07", 1.0, 7.5, 100, "/poster.jpg", List.of(), List.of());
    }

    public static TmdbTvShowDto tvShow(long id) {
        return tvShow(id, "Test TV Show " + id);
    }

    public static TmdbTvShowDto tvShow(long id, String name) {
        return new TmdbTvShowDto(id, name, "Overview", "2026-04-07", 1.0, 8.0, 150, "/poster.jpg", List.of(), List.of());
    }

    public static TmdbMovieDetailsDto movieDetails(long id) {
        return new TmdbMovieDetailsDto(id, "Test Movie", "Overview", "2026-04-07", 1.0, 7.5, 100,
                "/poster.jpg", "/backdrop.jpg", List.of(), credits());
    }

    public static TmdbTvShowDetailsDto tvShowDetails(long id) {
        return new TmdbTvShowDetailsDto(id, "Test TV Show", "Overview", "2026-04-07", 1.0, 8.0, 150,
                "/poster.jpg", "/backdrop.jpg", List.of(), credits());
    }

    public static TmdbChangesDto change(long id) {
        return new TmdbChangesDto(id, false);
    }

    public static <T> TmdbPagedResponseDto<T> page(int page, List<T> results, int totalPages, int totalResults) {
        return new TmdbPagedResponseDto<>(page, results, totalPages, totalResults);
    }

    public static TmdbPagedResponseDto<TmdbMovieDto> moviePage(int page, TmdbMovieDto... results) {
        return page(page, List.of(results), Math.max(page, 1), results.length);
    }

    public static TmdbPagedResponseDto<TmdbTvShowDto> tvPage(int page, TmdbTvShowDto... results) {
        return page(page, List.of(results), Math.max(page, 1), results.length);
    }

    public static TmdbPagedResponseDto<TmdbChangesDto> changePage(int page, TmdbChangesDto... results) {
        return page(page, List.of(results), Math.max(page, 1), results.length);
    }

    public static TmdbGenreListDto emptyGenres() { return new TmdbGenreListDto(List.of()); }
}