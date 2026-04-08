package org.tvl.tvlooker.persistence.tmdb;

import lombok.Getter;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMediaDetails;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMediaItem;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbMovieDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDetailsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbTvShowDto;

@Getter
public enum TmdbMediaType {
    MOVIE("movie", TmdbMovieDto.class, TmdbMovieDetailsDto.class),
    TV("tv", TmdbTvShowDto.class, TmdbTvShowDetailsDto.class);

    private final String path;
    private final Class<? extends TmdbMediaItem> mediaItemClass;
    private final Class<? extends TmdbMediaDetails> mediaDetailsClass;

    TmdbMediaType(
            String path,
            Class<? extends TmdbMediaItem> mediaItemClass,
            Class<? extends TmdbMediaDetails> mediaDetailsClass) {
        this.path = path;
        this.mediaItemClass = mediaItemClass;
        this.mediaDetailsClass = mediaDetailsClass;

    }
}