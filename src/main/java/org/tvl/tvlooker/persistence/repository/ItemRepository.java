package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.enums.TmdbType;

import java.util.Optional;

/**
 * Repository for Item entity persistence operations.
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    /**
     * Finds an item by its TMDB ID and type (MOVIE or TV).
     */
    Optional<Item> findByTmdbIdAndTmdbType(Long tmdbId, TmdbType tmdbType);

    /**
     * Checks if an item exists by its TMDB ID and type.
     */
    boolean existsByTmdbIdAndTmdbType(Long tmdbId, TmdbType tmdbType);
}

