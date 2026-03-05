package org.tvl.tvlooker.domain.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an actor in the system, with fields for the actor's ID, name, and TMDB ID.
 */
@Entity
@Table(name = "actors")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Actor {
    /**
     * The unique identifier for the actor, generated as a Long.
     * This field is the primary key of the "actors" table and is not updatable or nullable.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "actor_id_pk", updatable = false, nullable = false)
    private Long id;

    /**
     * The name of the actor, stored as a string.
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * The TMDB ID of the actor, stored as a Long.
     * This field is unique and cannot be null.
     */
    @Column(name = "tmdb_id", nullable = false, unique = true)
    private Long tmdbId;
}
