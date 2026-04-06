package org.tvl.tvlooker.domain.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the relationship between an Actor and an Item (movie/TV show).
 * Captures additional metadata like character name and billing order.
 * <p>
 * This join entity replaces the simple @ManyToMany relationship,
 * allowing us to store character names and billing order from TMDB API.
 * <p>
 * Example: In "Iron Man", Robert Downey Jr. played "Tony Stark" as the 1st billed actor.
 */
@Entity
@Table(name = "actors_items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActorItemEntity {
    /**
     * Unique identifier for this actor-item relationship.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "actor_item_id_pk", updatable = false, nullable = false)
    private Long id;

    /**
     * The item (movie or TV show) this actor appeared in.
     * Many ActorItems can reference the same Item.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id_fk", nullable = false)
    private ItemEntity item;

    /**
     * The actor that appeared in the item.
     * Many ActorItems can reference the same Actor.
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "actor_id_fk", nullable = false)
    private ActorEntity actor;

    /**
     * The character name the actor played in this item.
     * Example: "Tony Stark", "Peter Parker", "Pepper Potts"
     *
     * Nullable because some credits don't have character information.
     */
    @Column(name = "character_name", length = 255)
    private String characterName;

    /**
     * The billing order of this actor in the item's credits.
     * 0 = first billed, 1 = second billed, etc.
     *
     * Not nullable to ensure ordering consistency.
     */
    @Column(name = "billing_order", nullable = false)
    private Integer billingOrder;
}
