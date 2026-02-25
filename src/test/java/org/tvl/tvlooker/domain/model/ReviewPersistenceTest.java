package org.tvl.tvlooker.domain.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.enums.TmdbType;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de persistencia para la entidad Review utilizando JDBC.
 * Estas pruebas verifican que las entidades se persistan correctamente en la base de datos
 * sin utilizar repositorios JPA, usando directamente JdbcTemplate.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-02-25
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class ReviewPersistenceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID testUserId;
    private Long testItemId;
    private Long testReviewId;

    /**
     * Configuración inicial antes de cada prueba.
     * Limpia las tablas y prepara datos de prueba.
     */
    @BeforeEach
    void setUp() {
        // Limpiar tablas antes de cada prueba
        jdbcTemplate.execute("DELETE FROM reviews");
        jdbcTemplate.execute("DELETE FROM items");
        jdbcTemplate.execute("DELETE FROM users");

        // Reiniciar secuencias (H2)
        jdbcTemplate.execute("ALTER TABLE reviews ALTER COLUMN review_id_pk RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE items ALTER COLUMN item_id_pk RESTART WITH 1");
    }

    /**
     * Limpieza después de cada prueba.
     */
    @AfterEach
    void tearDown() {
        // Limpiar datos de prueba
        jdbcTemplate.execute("DELETE FROM reviews");
        jdbcTemplate.execute("DELETE FROM items");
        jdbcTemplate.execute("DELETE FROM users");
    }

    /**
     * Prueba la inserción de un usuario en la base de datos usando JDBC.
     */
    @Test
    @DisplayName("Debe persistir un usuario correctamente usando JDBC")
    void testPersistUser() {
        // Arrange
        String username = "testuser";
        String password = "password123";
        Timestamp createdAt = new Timestamp(System.currentTimeMillis());

        // Act
        jdbcTemplate.update(
                "INSERT INTO users (user_id_pk, username, password, created_at) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), username, password, createdAt
        );

        // Assert
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                username
        );

        assertNotNull(count);
        assertEquals(1, count, "Debe existir exactamente un usuario con ese username");

        // Verificar los datos del usuario
        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT username, password, created_at FROM users WHERE username = ?",
                username
        );

        assertEquals(username, user.get("username"));
        assertEquals(password, user.get("password"));
        assertNotNull(user.get("created_at"));
    }

    /**
     * Prueba la inserción de un item (película/serie) en la base de datos usando JDBC.
     */
    @Test
    @DisplayName("Debe persistir un item correctamente usando JDBC")
    void testPersistItem() {
        // Arrange
        Long tmdbId = 12345L;
        String tmdbType = "MOVIE";
        String title = "The Matrix";
        String overview = "A computer hacker learns about the true nature of reality.";
        LocalDate releaseDate = LocalDate.of(1999, 3, 31);
        BigDecimal popularity = new BigDecimal("850.1234");
        BigDecimal voteAverage = new BigDecimal("8.71");
        Timestamp createdAt = new Timestamp(System.currentTimeMillis());

        // Act
        jdbcTemplate.update(
                "INSERT INTO items (tmdb_id, tmdb_type, title, overview, release_date, popularity, vote_average, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                tmdbId, tmdbType, title, overview, releaseDate, popularity, voteAverage, createdAt
        );

        // Assert
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM items WHERE tmdb_id = ?",
                Integer.class,
                tmdbId
        );

        assertNotNull(count);
        assertEquals(1, count, "Debe existir exactamente un item con ese tmdb_id");

        // Verificar los datos del item
        Map<String, Object> item = jdbcTemplate.queryForMap(
                "SELECT tmdb_id, tmdb_type, title, overview, release_date, popularity, vote_average FROM items WHERE tmdb_id = ?",
                tmdbId
        );

        assertEquals(tmdbId, item.get("tmdb_id"));
        assertEquals(tmdbType, item.get("tmdb_type"));
        assertEquals(title, item.get("title"));
        assertEquals(overview, item.get("overview"));
        assertNotNull(item.get("release_date"));
    }

    /**
     * Prueba la inserción de una review completa con usuario e item usando JDBC.
     */
    @Test
    @DisplayName("Debe persistir una review completa con usuario e item usando JDBC")
    void testPersistCompleteReview() {
        // Arrange - Crear usuario
        UUID userId = UUID.randomUUID();
        String username = "reviewer123";
        String password = "securepass";
        Timestamp userCreatedAt = new Timestamp(System.currentTimeMillis());

        jdbcTemplate.update(
                "INSERT INTO users (user_id_pk, username, password, created_at) VALUES (?, ?, ?, ?)",
                userId, username, password, userCreatedAt
        );

        // Arrange - Crear item
        Long tmdbId = 550L;
        String tmdbType = "MOVIE";
        String title = "Fight Club";
        String overview = "An insomniac office worker forms an underground fight club.";
        LocalDate releaseDate = LocalDate.of(1999, 10, 15);
        BigDecimal popularity = new BigDecimal("920.5678");
        BigDecimal voteAverage = new BigDecimal("8.43");
        Timestamp itemCreatedAt = new Timestamp(System.currentTimeMillis());

        jdbcTemplate.update(
                "INSERT INTO items (tmdb_id, tmdb_type, title, overview, release_date, popularity, vote_average, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                tmdbId, tmdbType, title, overview, releaseDate, popularity, voteAverage, itemCreatedAt
        );

        // Obtener el ID generado del item
        Long itemId = jdbcTemplate.queryForObject(
                "SELECT item_id_pk FROM items WHERE tmdb_id = ?",
                Long.class,
                tmdbId
        );

        // Arrange - Crear review
        String reviewText = "Una película extraordinaria que te hace reflexionar sobre la sociedad moderna.";
        int score = 9;

        // Act - Insertar review
        jdbcTemplate.update(
                "INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                reviewText, score, itemId, userId
        );

        // Assert - Verificar que la review se insertó
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews WHERE item_id_fk = ? AND user_id_fk = ?",
                Integer.class,
                itemId, userId
        );

        assertNotNull(count);
        assertEquals(1, count, "Debe existir exactamente una review para ese item y usuario");

        // Verificar los datos de la review con JOIN
        Map<String, Object> result = jdbcTemplate.queryForMap(
                "SELECT r.review_text, r.score, u.username, i.title " +
                "FROM reviews r " +
                "JOIN users u ON r.user_id_fk = u.user_id_pk " +
                "JOIN items i ON r.item_id_fk = i.item_id_pk " +
                "WHERE r.item_id_fk = ? AND r.user_id_fk = ?",
                itemId, userId
        );

        assertEquals(reviewText, result.get("review_text"));
        assertEquals(score, result.get("score"));
        assertEquals(username, result.get("username"));
        assertEquals(title, result.get("title"));
    }

    /**
     * Prueba la persistencia de múltiples reviews para el mismo item.
     */
    @Test
    @DisplayName("Debe persistir múltiples reviews para el mismo item")
    void testPersistMultipleReviewsForSameItem() {
        // Arrange - Crear item
        Long tmdbId = 680L;
        String tmdbType = "MOVIE";
        String title = "Pulp Fiction";
        Timestamp itemCreatedAt = new Timestamp(System.currentTimeMillis());

        jdbcTemplate.update(
                "INSERT INTO items (tmdb_id, tmdb_type, title, overview, release_date, popularity, vote_average, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                tmdbId, tmdbType, title, "Película de Tarantino",
                LocalDate.of(1994, 10, 14), new BigDecimal("800"), new BigDecimal("8.50"), itemCreatedAt
        );

        Long itemId = jdbcTemplate.queryForObject(
                "SELECT item_id_pk FROM items WHERE tmdb_id = ?",
                Long.class,
                tmdbId
        );

        // Arrange - Crear usuarios
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();
        Timestamp userCreatedAt = new Timestamp(System.currentTimeMillis());

        jdbcTemplate.update("INSERT INTO users (user_id_pk, username, password, created_at) VALUES (?, ?, ?, ?)",
                userId1, "user1", "pass1", userCreatedAt);
        jdbcTemplate.update("INSERT INTO users (user_id_pk, username, password, created_at) VALUES (?, ?, ?, ?)",
                userId2, "user2", "pass2", userCreatedAt);
        jdbcTemplate.update("INSERT INTO users (user_id_pk, username, password, created_at) VALUES (?, ?, ?, ?)",
                userId3, "user3", "pass3", userCreatedAt);

        // Act - Insertar múltiples reviews
        jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                "Obra maestra del cine", 10, itemId, userId1);
        jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                "Muy buena pero no perfecta", 8, itemId, userId2);
        jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                "Excelente narrativa", 9, itemId, userId3);

        // Assert
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews WHERE item_id_fk = ?",
                Integer.class,
                itemId
        );

        assertEquals(3, count, "Deben existir 3 reviews para el item");

        // Verificar el promedio de scores
        Double avgScore = jdbcTemplate.queryForObject(
                "SELECT AVG(CAST(score AS DOUBLE)) FROM reviews WHERE item_id_fk = ?",
                Double.class,
                itemId
        );

        assertNotNull(avgScore);
        assertEquals(9.0, avgScore, 0.1, "El promedio de scores debe ser 9.0");
    }

    /**
     * Prueba la persistencia de una review con score mínimo y sin texto.
     */
    @Test
    @DisplayName("Debe persistir una review sin texto (solo score)")
    void testPersistReviewWithoutText() {
        // Arrange
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users (user_id_pk, username, password, created_at) VALUES (?, ?, ?, ?)",
                userId, "quickrater", "pass", new Timestamp(System.currentTimeMillis()));

        Long tmdbId = 155L;
        jdbcTemplate.update(
                "INSERT INTO items (tmdb_id, tmdb_type, title, overview, release_date, popularity, vote_average, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                tmdbId, "MOVIE", "The Dark Knight", "Batman",
                LocalDate.of(2008, 7, 18), new BigDecimal("900"), new BigDecimal("9.00"),
                new Timestamp(System.currentTimeMillis())
        );

        Long itemId = jdbcTemplate.queryForObject(
                "SELECT item_id_pk FROM items WHERE tmdb_id = ?",
                Long.class,
                tmdbId
        );

        // Act - Insertar review sin texto (NULL)
        jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                null, 10, itemId, userId);

        // Assert
        Map<String, Object> review = jdbcTemplate.queryForMap(
                "SELECT review_text, score FROM reviews WHERE item_id_fk = ? AND user_id_fk = ?",
                itemId, userId
        );

        assertEquals(null, review.get("review_text"), "El texto de la review debe ser null");
        assertEquals(10, review.get("score"), "El score debe ser 10");
    }

    /**
     * Prueba la integridad referencial - no debe permitir insertar una review sin usuario válido.
     */
    @Test
    @DisplayName("Debe fallar al insertar review con usuario inexistente")
    void testFailPersistReviewWithInvalidUser() {
        // Arrange - Crear solo el item, sin usuario
        Long tmdbId = 278L;
        jdbcTemplate.update(
                "INSERT INTO items (tmdb_id, tmdb_type, title, overview, release_date, popularity, vote_average, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                tmdbId, "MOVIE", "The Shawshank Redemption", "Redemption",
                LocalDate.of(1994, 9, 23), new BigDecimal("950"), new BigDecimal("9.30"),
                new Timestamp(System.currentTimeMillis())
        );

        Long itemId = jdbcTemplate.queryForObject(
                "SELECT item_id_pk FROM items WHERE tmdb_id = ?",
                Long.class,
                tmdbId
        );

        UUID fakeUserId = UUID.randomUUID();

        // Act & Assert - Debe lanzar una excepción
        try {
            jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                    "Esta review no debería insertarse", 5, itemId, fakeUserId);

            // Si llegamos aquí, la prueba falla porque debería haber lanzado una excepción
            assertTrue(false, "Debería haber lanzado una excepción por violación de clave foránea");
        } catch (Exception e) {
            // Verificar que es una excepción de integridad referencial
            String errorMessage = e.getMessage().toLowerCase();
            assertTrue(errorMessage.contains("foreign key") ||
                      errorMessage.contains("referential integrity") ||
                      errorMessage.contains("constraint"),
                    "Debe ser una excepción de integridad referencial");
        }
    }

    /**
     * Prueba consultas complejas con múltiples tablas.
     */
    @Test
    @DisplayName("Debe ejecutar consultas complejas con JOINs correctamente")
    void testComplexQueryWithJoins() {
        // Arrange - Crear datos de prueba
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        jdbcTemplate.update("INSERT INTO users (user_id_pk, username, password, created_at) VALUES (?, ?, ?, ?)",
                userId1, "critic1", "pass", timestamp);
        jdbcTemplate.update("INSERT INTO users (user_id_pk, username, password, created_at) VALUES (?, ?, ?, ?)",
                userId2, "critic2", "pass", timestamp);

        // Crear dos items
        jdbcTemplate.update(
                "INSERT INTO items (tmdb_id, tmdb_type, title, overview, release_date, popularity, vote_average, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                101L, "MOVIE", "Inception", "Dreams", LocalDate.of(2010, 7, 16),
                new BigDecimal("900"), new BigDecimal("8.80"), timestamp
        );
        jdbcTemplate.update(
                "INSERT INTO items (tmdb_id, tmdb_type, title, overview, release_date, popularity, vote_average, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                102L, "TV", "Breaking Bad", "Chemistry teacher", LocalDate.of(2008, 1, 20),
                new BigDecimal("950"), new BigDecimal("9.50"), timestamp
        );

        Long itemId1 = jdbcTemplate.queryForObject("SELECT item_id_pk FROM items WHERE tmdb_id = ?", Long.class, 101L);
        Long itemId2 = jdbcTemplate.queryForObject("SELECT item_id_pk FROM items WHERE tmdb_id = ?", Long.class, 102L);

        // Crear reviews
        jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                "Mind-bending", 9, itemId1, userId1);
        jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                "Best TV show ever", 10, itemId2, userId1);
        jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                "Great movie", 8, itemId1, userId2);

        // Act - Consulta compleja: obtener todas las reviews con información del usuario e item
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT u.username, i.title, i.tmdb_type, r.review_text, r.score " +
                "FROM reviews r " +
                "INNER JOIN users u ON r.user_id_fk = u.user_id_pk " +
                "INNER JOIN items i ON r.item_id_fk = i.item_id_pk " +
                "ORDER BY r.score DESC"
        );

        // Assert
        assertNotNull(results);
        assertEquals(3, results.size(), "Deben existir 3 reviews en total");

        // Verificar el primer resultado (mayor score)
        Map<String, Object> topReview = results.get(0);
        assertEquals(10, topReview.get("score"));
        assertEquals("Best TV show ever", topReview.get("review_text"));
        assertEquals("Breaking Bad", topReview.get("title"));
    }

    /**
     * Prueba la actualización de una review existente.
     */
    @Test
    @DisplayName("Debe actualizar una review existente correctamente")
    void testUpdateReview() {
        // Arrange - Crear datos iniciales
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users (user_id_pk, username, password, created_at) VALUES (?, ?, ?, ?)",
                userId, "updater", "pass", new Timestamp(System.currentTimeMillis()));

        Long tmdbId = 999L;
        jdbcTemplate.update(
                "INSERT INTO items (tmdb_id, tmdb_type, title, overview, release_date, popularity, vote_average, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                tmdbId, "MOVIE", "Test Movie", "Test", LocalDate.now(),
                new BigDecimal("100"), new BigDecimal("7.00"), new Timestamp(System.currentTimeMillis())
        );

        Long itemId = jdbcTemplate.queryForObject("SELECT item_id_pk FROM items WHERE tmdb_id = ?", Long.class, tmdbId);

        jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                "Original review", 7, itemId, userId);

        Long reviewId = jdbcTemplate.queryForObject(
                "SELECT review_id_pk FROM reviews WHERE item_id_fk = ? AND user_id_fk = ?",
                Long.class, itemId, userId
        );

        // Act - Actualizar la review
        String updatedText = "Updated review after second viewing";
        int updatedScore = 9;

        jdbcTemplate.update(
                "UPDATE reviews SET review_text = ?, score = ? WHERE review_id_pk = ?",
                updatedText, updatedScore, reviewId
        );

        // Assert
        Map<String, Object> review = jdbcTemplate.queryForMap(
                "SELECT review_text, score FROM reviews WHERE review_id_pk = ?",
                reviewId
        );

        assertEquals(updatedText, review.get("review_text"));
        assertEquals(updatedScore, review.get("score"));
    }

    /**
     * Prueba la eliminación de una review.
     */
    @Test
    @DisplayName("Debe eliminar una review correctamente")
    void testDeleteReview() {
        // Arrange
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users (user_id_pk, username, password, created_at) VALUES (?, ?, ?, ?)",
                userId, "deleter", "pass", new Timestamp(System.currentTimeMillis()));

        Long tmdbId = 888L;
        jdbcTemplate.update(
                "INSERT INTO items (tmdb_id, tmdb_type, title, overview, release_date, popularity, vote_average, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                tmdbId, "TV", "Test TV", "Test", LocalDate.now(),
                new BigDecimal("100"), new BigDecimal("7.00"), new Timestamp(System.currentTimeMillis())
        );

        Long itemId = jdbcTemplate.queryForObject("SELECT item_id_pk FROM items WHERE tmdb_id = ?", Long.class, tmdbId);

        jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                "Review to be deleted", 6, itemId, userId);

        Long reviewId = jdbcTemplate.queryForObject(
                "SELECT review_id_pk FROM reviews WHERE item_id_fk = ? AND user_id_fk = ?",
                Long.class, itemId, userId
        );

        // Act - Eliminar la review
        jdbcTemplate.update("DELETE FROM reviews WHERE review_id_pk = ?", reviewId);

        // Assert
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews WHERE review_id_pk = ?",
                Integer.class,
                reviewId
        );

        assertEquals(0, count, "La review debe haber sido eliminada");

        // Verificar que el usuario e item siguen existiendo
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE user_id_pk = ?",
                Integer.class,
                userId
        );
        assertEquals(1, userCount, "El usuario debe seguir existiendo");
    }
}

