package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tvl.tvlooker.domain.model.entity.ActorItemEntity;

/**
 * Repository for ActorItemEntity (actor-item relationships).
 */
interface ActorItemRepository extends JpaRepository<ActorItemEntity, Long> {
}
