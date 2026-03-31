package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tvl.tvlooker.domain.model.entity.ListFavoriteEntity;

import java.util.List;
import java.util.UUID;

public interface ListFavoriteRepository extends JpaRepository<ListFavoriteEntity, Long> {
    List<ListFavoriteEntity> findByUserId(UUID userId);
}
