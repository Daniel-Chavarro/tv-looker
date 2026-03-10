package org.tvl.tvlooker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tvl.tvlooker.domain.model.entity.Director;

import java.util.Optional;

/**
 * Repository for Director entity persistence operations.
 */
@Repository
public interface DirectorRepository extends JpaRepository<Director, Long> {

    /**
     * Finds a director by their TMDB ID.
     */
    Optional<Director> findByTmdbId(Long tmdbId);
}

