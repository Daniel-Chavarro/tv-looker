package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tvl.tvlooker.domain.model.entity.Actor;

import java.util.Optional;

/**
 * Repository for Actor entity persistence operations.
 */
@Repository
public interface ActorRepository extends JpaRepository<Actor, Long> {

    /**
     * Finds an actor by their TMDB ID.
     */
    Optional<Actor> findByTmdbId(Long tmdbId);
}


