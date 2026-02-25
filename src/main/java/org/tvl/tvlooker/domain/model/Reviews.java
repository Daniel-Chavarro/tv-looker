package org.tvl.tvlooker.domain.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a review in the system, containing information about the review text, score, the item being reviewed, and the user who wrote the review.
 * Each review is associated with a specific item and user, and contains a score that represents the rating given by the user.
 */
@Entity
@Table(name = "reviews")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Reviews {
    /**
     * The unique identifier for the review, generated as a Long.
     * This field is the primary key of the "reviews" table and is not updatable or nullable.
     */
    @Id
    @Column(name = "review_id_pk", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The text of the review, stored as a string.
     * This field is optional and can be null, allowing users to provide a review without text if they choose.
     */
    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    /**
     * The score given in the review, stored as an integer.
     * This field is not nullable and represents the rating provided by the user.
     */
    @Column(name = "score", nullable = false)
    private int score;

    /**
     * The item being reviewed, represented as a many-to-one relationship with the Item entity.
     * This field is not nullable and is linked to the "items" table via a foreign key.
     */
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "item_id_fk", nullable = false)
    private Item item;

    /**
     * The user who wrote the review, represented as a many-to-one relationship with the User entity.
     * This field is not nullable and is linked to the "users" table via a foreign key.
     */
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id_fk", nullable = false)
    private User user;
}
