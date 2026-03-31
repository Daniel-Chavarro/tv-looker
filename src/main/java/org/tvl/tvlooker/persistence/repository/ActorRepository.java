package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;

import java.util.Optional;

/**
 * Repository for Actor entity persistence operations.
 */
@Repository
public interface ActorRepository extends JpaRepository<ActorEntity, Long> {

    /**
     * Finds an actor by their TMDB ID.
     */
    Optional<ActorEntity> findByTmdbId(Long tmdbId);
}


