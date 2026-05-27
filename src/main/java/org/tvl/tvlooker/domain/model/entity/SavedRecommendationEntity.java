package org.tvl.tvlooker.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "saved_recommendations",
        indexes = {
                @Index(name = "idx_saved_recommendations_user_expires", columnList = "user_id_fk, expires_at"),
                @Index(name = "idx_saved_recommendations_user_rank", columnList = "user_id_fk, rank_position")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class SavedRecommendationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id_pk", updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_fk", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id_fk", nullable = false)
    private ItemEntity item;

    @Column(name = "score", nullable = false)
    private double score;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "source_strategy")
    private String sourceStrategy;

    @Column(name = "rank_position", nullable = false)
    private int rankPosition;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
