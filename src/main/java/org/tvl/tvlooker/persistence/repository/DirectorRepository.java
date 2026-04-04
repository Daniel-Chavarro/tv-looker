package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository for Director entity persistence operations.
 */
@Repository
public interface DirectorRepository extends JpaRepository<DirectorEntity, Long> {

    /**
     * Finds a director by their TMDB ID.
     */
    Optional<DirectorEntity> findByTmdbId(Long tmdbId);

    /**
     * Finds all directors with TMDB IDs in the provided set.
     * @param tmdbIds Set of TMDB IDs to search for.
     * @return List of directors matching the criteria.
     */
    @Query("SELECT d FROM DirectorEntity d WHERE d.tmdbId IN :tmdbIds")
    List<DirectorEntity> findAllByTmdbIdIn(@Param("tmdbIds") Set<Long> tmdbIds);
}

