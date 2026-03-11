package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tvl.tvlooker.domain.model.entity.Interaction;

import java.util.List;
import java.util.UUID;

interface InteractionRepository extends JpaRepository<Interaction, Long> {
    List<Interaction> findByUser_Id(UUID userId);
}
