package org.tvl.tvlooker.domain.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.tvl.tvlooker.domain.model.enums.TmdbType;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Set;

/**
 * Represents an item in the system, which can be either a movie or a TV show.
 * The item is identified by a unique ID and contains information such as title, overview, release date, popularity, and
 * vote average.
 */
@Entity
@Table(name = "items")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Item {

    /**
     * The unique identifier for the item, generated as a Long.
     * This field is the primary key of the "items" table and is not updatable or nullable.
     */
    @Id
    @Column(name = "item_id_pk", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The TMDB ID of the item, stored as a Long.
     * This field is unique and cannot be null.
     */
    @Column(name = "tmdb_id", nullable = false, unique = true)
    private Long tmdbId;

    /**
     * The type of the item, which can be either MOVIE or TV, stored as an enum.
     * This field is not nullable and is stored as a string in the database.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tmdb_type", nullable = false)
    private TmdbType tmdbType;

    /**
     * The title of the item, stored as a string.
     */
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * The overview of the item, stored as a string.
     */
    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;

    /**
     * The release date of the item, stored as a LocalDate.
     */
    @Column(name = "release_date")
    private LocalDate releaseDate;

    /**
     * The popularity of the item, stored as a BigDecimal.
     */
    @Column(name = "popularity", precision = 10, scale = 4)
    private BigDecimal popularity;

    /**
     * The average vote of the item, stored as a BigDecimal.
     */
    @Column(name = "vote_average", precision = 3, scale = 2)
    private BigDecimal voteAverage;

    /**
     * The timestamp when the item was created, stored as a Timestamp.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    /**
     * The genres associated with the item, represented as a many-to-many relationship with the Genre entity.
     * This field is lazily loaded and uses a join table named "genres_items" to link items and genres.
     */
    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(
            name = "genres_items",
            joinColumns = @JoinColumn(name = "item_id_fk"),
            inverseJoinColumns = @JoinColumn(name = "genre_id_fk")
    )
    private Set<Genre> genres;

    /**
     * The directors associated with the item, represented as a many-to-many relationship with the Director entity.
     * This field is lazily loaded and uses a join table named "directors_items" to link items and directors.
     */
    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(
            name = "directors_items",
            joinColumns = @JoinColumn(name = "item_id_fk"),
            inverseJoinColumns = @JoinColumn(name = "director_id_fk")
    )
    private Set<Director> directors;

    /**
     * The actors associated with the item, represented as a many-to-many relationship with the Actor entity.
     * This field is lazily loaded and uses a join table named "actors_items" to link items and actors.
     */
    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(
            name = "actors_items",
            joinColumns = @JoinColumn(name = "item_id_fk"),
            inverseJoinColumns = @JoinColumn(name = "actor_id_fk")
    )
    private Set<Actor> actors;
}
