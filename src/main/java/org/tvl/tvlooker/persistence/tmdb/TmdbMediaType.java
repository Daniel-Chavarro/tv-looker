package org.tvl.tvlooker.persistence.tmdb;

import lombok.Getter;

@Getter
public enum TmdbMediaType {
    MOVIE("movie"),
    TV("tv");

    private final String path;

    TmdbMediaType(String path) {
        this.path = path;
    }

    public String getGenresEndpoint() {
        return path + "/genre/list";
    }
}