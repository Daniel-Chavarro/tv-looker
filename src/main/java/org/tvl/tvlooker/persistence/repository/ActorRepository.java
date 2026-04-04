package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository for Actor entity persistence operations.
 */
@Repository
public interface ActorRepository extends JpaRepository<ActorEntity, Long> {

    /**
     * Finds an actor by their TMDB ID.
     */
    Optional<ActorEntity> findByTmdbId(Long tmdbId);

    /**
     * Finds all actors with TMDB IDs in the provided set.
     * @param tmdbIds Set of TMDB IDs to search for.
     * @return List of actors matching the criteria.
     */
    @Query("SELECT a FROM ActorEntity a WHERE a.tmdbId IN :tmdbIds")
    List<ActorEntity> findAllByTmdbIdIn(@Param("tmdbIds") Set<Long> tmdbIds);
}


