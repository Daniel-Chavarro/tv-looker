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
 * Represents a genre in the system, with fields for the genre's ID and name.
 */
@Entity
@Table(name = "genres")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Genre {
    /**
     * The unique identifier for the genre, generated as a Long.
     * This field is the primary key of the "genres" table and is not updatable or nullable.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "genre_id_pk", updatable = false, nullable = false)
    private Long id;

    /**
     * The name of the genre, stored as a string.
     */
    @Column(name = "name", nullable = false)
    private String name;
}
