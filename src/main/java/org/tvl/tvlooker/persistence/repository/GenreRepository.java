package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;

import java.util.Optional;

/**
 * Repository for Genre entity persistence operations.
 */
@Repository
public interface GenreRepository extends JpaRepository<GenreEntity, Long> {

    /**
     * Finds a genre by its TMDB ID.
     */
    Optional<GenreEntity> findByTmdbId(Long tmdbId);
}

