package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tvl.tvlooker.domain.model.entity.ListFavorite;

import java.util.List;
import java.util.UUID;

interface ListFavoriteRepository extends JpaRepository<ListFavorite, Long> {
    List<ListFavorite> findByUserId(UUID userId);
}
