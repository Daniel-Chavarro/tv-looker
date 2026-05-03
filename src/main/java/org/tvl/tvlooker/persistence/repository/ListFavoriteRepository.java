package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.tvl.tvlooker.domain.model.entity.ListFavoriteEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListFavoriteRepository extends JpaRepository<ListFavoriteEntity, Long> {
    List<ListFavoriteEntity> findByUserId(UUID userId);

    Optional<ListFavoriteEntity> findByIdAndUserId(Long id, UUID userId);
    
    Page<ListFavoriteEntity> findAllByUserId(UUID userId, Pageable pageable);
}
