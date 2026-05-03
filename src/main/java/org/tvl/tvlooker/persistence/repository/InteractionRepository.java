package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.tvl.tvlooker.domain.model.entity.InteractionEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InteractionRepository extends JpaRepository<InteractionEntity, Long> {
    List<InteractionEntity> findByUserId(UUID userId);

    Optional<InteractionEntity> findByIdAndUserId(Long id, UUID userId);

    Page<InteractionEntity> findAllByUserId(UUID userId, Pageable pageable);
}
