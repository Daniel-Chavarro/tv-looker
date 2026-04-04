package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Repository for Genre entity persistence operations.
 */
@Repository
public interface GenreRepository extends JpaRepository<GenreEntity, Long> {

    /**
     * Finds a genre by its TMDB ID.
     */
    Optional<GenreEntity> findByTmdbId(Long tmdbId);

    /**
     * Finds all genres with TMDB IDs in the provided set.
     * @param tmdbIds Set of TMDB IDs to search for.
     * @return List of genres matching the criteria.
     */
    @Query("SELECT g FROM GenreEntity g WHERE g.tmdbId IN :tmdbIds")
    List<GenreEntity> findAllByTmdbIdIn(@Param("tmdbIds") Set<Long> tmdbIds);
}

