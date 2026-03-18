package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;

import java.util.Optional;

/**
 * Repository for Director entity persistence operations.
 */
@Repository
public interface DirectorRepository extends JpaRepository<DirectorEntity, Long> {

    /**
     * Finds a director by their TMDB ID.
     */
    Optional<DirectorEntity> findByTmdbId(Long tmdbId);
}

