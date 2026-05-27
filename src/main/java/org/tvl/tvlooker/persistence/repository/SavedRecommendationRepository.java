package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.tvl.tvlooker.domain.model.entity.SavedRecommendationEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SavedRecommendationRepository extends JpaRepository<SavedRecommendationEntity, Long> {

    @Query("SELECT sr FROM SavedRecommendationEntity sr WHERE sr.user.id = :userId AND sr.expiresAt > :now ORDER BY sr.rankPosition ASC")
    List<SavedRecommendationEntity> findFreshByUserIdOrderByRankPositionAsc(UUID userId, Instant now);

    @Modifying
    @Query("DELETE FROM SavedRecommendationEntity sr WHERE sr.user.id = :userId")
    void deleteAllByUserId(UUID userId);
}
