package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tvl.tvlooker.domain.model.entity.Interaction;
import org.tvl.tvlooker.domain.model.entity.Review;

import java.util.List;
import java.util.UUID;

interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByUser_Id(UUID userId);
}
