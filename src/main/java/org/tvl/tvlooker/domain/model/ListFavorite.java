package org.tvl.tvlooker.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

/**
 * Represents a list of favorites in the system, with fields for the list's ID, name, description, and associated user.
 * The list is identified by a unique Long ID which serves as the primary key in the database.
 */
@Entity
@Table(name = "list_favorites")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ListFavorite {
    /**
     * The unique identifier for the list of favorites, generated as a Long.
     * This field is the primary key of the "list_favorites" table and is not updatable or nullable.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "list_favorite_id_pk", updatable = false, nullable = false)
    private Long id;

    /**
     * The name of the list of favorites, stored as a string.
     * This field is unique and cannot be null, with a maximum length of 30 characters.
     */
    @Column(name = "name", nullable = false, unique = true, length = 30)
    private String name;

    /**
     * The description of the list of favorites, stored as a string.
     * This field has a maximum length of 200 characters.
     */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * The user associated with the list of favorites, represented as a UUID.
     * This field is a foreign key referencing the "users" table and cannot be null.
     */
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_id_fk", nullable = false)
    private User user;

    /**
     * The set of items associated with the list of favorites, represented as a many-to-many relationship.
     * This field is mapped to the "favorite_items" join table, which contains foreign keys referencing both the
     * "list_favorites" and "items" tables.
     * <p>
     * The relationship is lazily loaded and cascades persist operations.
     */
    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(
            name = "favorite_items",
            joinColumns = @JoinColumn(name = "list_id_fk"),
            inverseJoinColumns = @JoinColumn(name = "item_id_fk")
    )
    private Set<Item> items;
}
