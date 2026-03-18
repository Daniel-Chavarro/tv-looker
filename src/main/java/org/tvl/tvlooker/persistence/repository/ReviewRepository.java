package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tvl.tvlooker.domain.model.entity.ReviewEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    List<ReviewEntity> findByUserId(UUID userId);

    Optional<ReviewEntity> findByUserIdAndItemId(UUID userId, Long itemId);
}
