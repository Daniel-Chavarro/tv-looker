package org.tvl.tvlooker.persistence.tmdb.dto;

import java.util.List;

public interface TmdbMediaItem {
    long id();
    String title();
    String overview();
    String releaseDate();
    double popularity();
    double voteAverage();
    int voteCount();
    String posterPath();
    List<Integer> genreIds();
    List<TmdbGenreDto> genres();
}
