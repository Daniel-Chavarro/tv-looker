package org.tvl.tvlooker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tvl.tvlooker.domain.model.entity.Genre;

import java.util.Optional;

/**
 * Repository for Genre entity persistence operations.
 */
@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {

    /**
     * Finds a genre by its TMDB ID.
     */
    Optional<Genre> findByTmdbId(Long tmdbId);
}

