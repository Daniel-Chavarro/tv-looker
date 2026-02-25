package org.tvl.tvlooker.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.UUID;


/**
 * Represents a user in the system, with fields for username, password, and creation timestamp.
 * The user is identified by a unique UUID which serves as the primary key in the database.
 */
@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class User {
    /**
     * The unique identifier for the user, generated as a UUID.
     * This field is the primary key of the "users" table and is not updatable or nullable.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id_pk", updatable = false, nullable = false)
    private UUID id;

    /**
     * The username of the user, stored as a string.
     */
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    /**
     * The password of the user, stored as a string.
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * The timestamp when the user was created, stored as a Timestamp.
     */
    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;
}
