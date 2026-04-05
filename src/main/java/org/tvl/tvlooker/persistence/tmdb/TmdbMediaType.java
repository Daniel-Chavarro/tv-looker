package org.tvl.tvlooker.persistence.tmdb;

public enum TmdbMediaType {
    MOVIE("movie"),
    TV("tv");

    private final String path;

    TmdbMediaType(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public String getGenresEndpoint() {
        return path + "/genre/list";
    }
}