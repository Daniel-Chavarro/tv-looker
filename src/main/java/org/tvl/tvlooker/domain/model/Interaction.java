package org.tvl.tvlooker.domain.model;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.tvl.tvlooker.domain.model.enums.InteractionType;

/**
 * Represents an interaction in the system, which can be a user interaction with an item (e.g., a review).
 * The interaction is identified by a unique ID and contains information such as the type of interaction,
 * the timestamp when it was created, and the associated user, item, and review.
 */
@Table
@Entity(name = "interactions")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Interaction {
    /**
     * The unique identifier for the interaction, generated as a Long.
     * This field is the primary key of the "interactions" table.
     */
    @Id
    @Column(name = "interaction_id_pk")
    private Long id;

    /**
     * The type of interaction, stored as an enum.
     * This field is not nullable and is stored as a string in the database.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false)
    private InteractionType interactionType;

    /**
     * The timestamp when the interaction was created, stored as a Timestamp.
     */
    @CreationTimestamp
    @Column(name = "created_at",  nullable = false)
    private Timestamp createdAt;

    /**
     * The user associated with the interaction, represented as a many-to-one relationship.
     * This field is not nullable and is linked to the "users" table via the "user_id_fk" foreign key.
     */
    @ManyToOne
    @JoinColumn(name = "user_id_fk", nullable = false)
    private User user;

    /**
     * The item associated with the interaction, represented as a many-to-one relationship.
     * This field is not nullable and is linked to the "items" table via the "item_id_pk" foreign key.
     */
    @ManyToOne
    @JoinColumn(name = "item_id_pk", nullable = false)
    private Item item;

    /**
     * The review associated with the interaction, represented as a many-to-one relationship.
     * This field is optional and can be null, allowing interactions that are not linked to a review.
     */
    @ManyToOne
    @JoinColumn(name = "review_id_pk")
    private Review review;

}