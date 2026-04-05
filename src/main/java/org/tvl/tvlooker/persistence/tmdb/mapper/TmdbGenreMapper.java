package org.tvl.tvlooker.persistence.tmdb.mapper;

import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;

public final class TmdbGenreMapper {

    private TmdbGenreMapper() {
    }

    public static GenreEntity toEntity(TmdbGenreDto dto) {
        GenreEntity genre = new GenreEntity();
        genre.setTmdbId((long) dto.id());
        genre.setName(dto.name());
        return genre;
    }
}
