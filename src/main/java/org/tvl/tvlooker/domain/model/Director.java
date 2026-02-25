package org.tvl.tvlooker.domain.model;

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
 * Represents a director in the system, with fields for the director's ID, name, and TMDB ID.
 */
@Entity
@Table(name = "directors")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Director {
    /**
     * The unique identifier for the director, generated as a BigInteger.
     * This field is the primary key of the "directors" table and is not updatable or nullable.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "director_id_pk", updatable = false, nullable = false)
    private Long id;

    /**
     * The name of the director, stored as a string.
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * The TMDB ID of the director, stored as a BigInteger.
     * This field is unique and cannot be null.
     */
    @Column(name = "tmdb_id", nullable = false, unique = true)
    private Long tmdbId;
}
