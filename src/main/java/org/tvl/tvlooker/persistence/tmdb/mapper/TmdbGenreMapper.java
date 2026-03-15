package org.tvl.tvlooker.persistence.tmdb.mapper;

import org.tvl.tvlooker.domain.model.entity.Genre;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.persistence.repository.GenreRepository;

/**
 * Maps TMDB genre DTOs to Genre JPA entities using find-or-create by tmdbId.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-10
 */
public final class TmdbGenreMapper {

    private TmdbGenreMapper() {
        // Utility class
    }

    /**
     * Finds an existing Genre by tmdbId or creates and saves a new one.
     *
     * @param dto        the TMDB genre DTO
     * @param repository the genre repository
     * @return the existing or newly created Genre entity
     */
    public static Genre findOrCreate(TmdbGenreDto dto, GenreRepository repository) {
        return repository.findByTmdbId((long) dto.id())
                .orElseGet(() -> {
                    Genre genre = new Genre();
                    genre.setTmdbId((long) dto.id());
                    genre.setName(dto.name());
                    return repository.save(genre);
                });
    }
}

