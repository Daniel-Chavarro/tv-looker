package org.tvl.tvlooker.domain.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.enums.TmdbType;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for testing the persistence of model entities using JDBC in a Spring Boot application.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-02-25
 */
@SpringBootTest
@DisplayName("Pruebas de Persistencia para Review con JDBC")
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class ReviewPersistenceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID testUserId;
    private Long testItemId;
    private Long testReviewId;

    /**
     * Initial setup before each test, ensuring a clean state for the database.
     */
    @BeforeEach
    void setUp() {

        jdbcTemplate.execute("DELETE FROM reviews");
        jdbcTemplate.execute("DELETE FROM items");
        jdbcTemplate.execute("DELETE FROM users");


        jdbcTemplate.execute("ALTER TABLE reviews ALTER COLUMN review_id_pk RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE items ALTER COLUMN item_id_pk RESTART WITH 1");
    }

    /**
     * Cleanup after each test, removing any test data from the database to maintain isolation between tests.
     */
    @AfterEach
    void tearDown() {
        // Limpiar datos de prueba
        jdbcTemplate.execute("DELETE FROM reviews");
        jdbcTemplate.execute("DELETE FROM items");
        jdbcTemplate.execute("DELETE FROM users");
    }

    /**
     * Test the insertion of a user into the database using JDBC, ensuring that the user is persisted correctly and can
     * be retrieved with the expected data.
     */
    @Test
    @DisplayName("Has to persist a user correctly using JDBC")
    void testPersistUser() {

        String username = "testuser";
        String password = "password123";
        Timestamp createdAt = new Timestamp(System.currentTimeMillis());


        jdbcTemplate.update(
                "INSERT INTO users (user_id_pk, username, password, created_at) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), username, password, createdAt
        );


        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = ?",
                Integer.class,
                username
        );

        assertNotNull(count);
        assertEquals(1, count, "Must exist exactly one user with that username");

        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT username, password, created_at FROM users WHERE username = ?",
                username
        );

        assertEquals(username, user.get("username"));
        assertEquals(password, user.get("password"));
        assertNotNull(user.get("created_at"));
    }

    /**
     * Test the insertion of an item into the database using JDBC, ensuring that the item is persisted correctly
     */
    @Test
    @DisplayName("Has to persist an item correctly using JDBC")
    void testPersistItem() {

        Long tmdbId = 12345L;
        String tmdbType = "MOVIE";
        String title = "The Matrix";
        String overview = "A computer hacker learns about the true nature of reality.";
        LocalDate releaseDate = LocalDate.of(1999, 3, 31);
        BigDecimal popularity = new BigDecimal("850.1234");
        BigDecimal voteAverage = new BigDecimal("8.71");
        Timestamp createdAt = new Timestamp(System.currentTimeMillis());


        jdbcTemplate.update(
                "INSERT INTO items (tmdb_id, tmdb_type, title, overview, release_date, popularity, vote_average, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                tmdbId, tmdbType, title, overview, releaseDate, popularity, voteAverage, createdAt
        );


        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM items WHERE tmdb_id = ?",
                Integer.class,
                tmdbId
        );

        assertNotNull(count);
        assertEquals(1, count, "Should exist exactly one item with that TMDB ID");


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
     * Test the insertion of a complete review into the database using JDBC, ensuring that the review is persisted
     * correctly and that the relationships with the user and item are maintained.
     */
    @Test
    @DisplayName("Must persist a complete review with user and item using JDBC")
    void testPersistCompleteReview() {
        UUID userId = UUID.randomUUID();
        String username = "reviewer123";
        String password = "securepass";
        Timestamp userCreatedAt = new Timestamp(System.currentTimeMillis());

        jdbcTemplate.update(
                "INSERT INTO users (user_id_pk, username, password, created_at) VALUES (?, ?, ?, ?)",
                userId, username, password, userCreatedAt
        );


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


        Long itemId = jdbcTemplate.queryForObject(
                "SELECT item_id_pk FROM items WHERE tmdb_id = ?",
                Long.class,
                tmdbId
        );


        String reviewText = "Una película extraordinaria que te hace reflexionar sobre la sociedad moderna.";
        int score = 9;

        // Act - Insertar review
        jdbcTemplate.update(
                "INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                reviewText, score, itemId, userId
        );


        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews WHERE item_id_fk = ? AND user_id_fk = ?",
                Integer.class,
                itemId, userId
        );

        assertNotNull(count);
        assertEquals(1, count, "Should exist exactly one review for that item and user");

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
     * Test the insertion of multiple reviews for the same item.
     */
    @Test
    @DisplayName("Must persist multiple reviews for the same item and calculate average score correctly")
    void testPersistMultipleReviewsForSameItem() {

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
        
        jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                "Obra maestra del cine", 10, itemId, userId1);
        jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                "Muy buena pero no perfecta", 8, itemId, userId2);
        jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                "Excelente narrativa", 9, itemId, userId3);
        
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews WHERE item_id_fk = ?",
                Integer.class,
                itemId
        );

        assertEquals(3, count, "Should exist exactly three reviews for that item");
        
        Double avgScore = jdbcTemplate.queryForObject(
                "SELECT AVG(CAST(score AS DOUBLE)) FROM reviews WHERE item_id_fk = ?",
                Double.class,
                itemId
        );

        assertNotNull(avgScore);
        assertEquals(9.0, avgScore, 0.1, 
                "The average score should be 9.0 based on the inserted reviews");
    }

    /**
     * Test the insertion of a review without text.
     */
    @Test
    @DisplayName("Should allow inserting a review without text (null review_text)")
    void testPersistReviewWithoutText() {
        
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

        jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                null, 10, itemId, userId);

        Map<String, Object> review = jdbcTemplate.queryForMap(
                "SELECT review_text, score FROM reviews WHERE item_id_fk = ? AND user_id_fk = ?",
                itemId, userId
        );

        assertNull(review.get("review_text"), "The review_text should be null");
        assertEquals(10, review.get("score"), "The score should be 10");
    }

    /**
     * Test the insertion of a review with an invalid user ID, expecting a foreign key constraint violation.
     */
    @Test
    @DisplayName("Should fail to insert a review with an invalid user ID (foreign key constraint violation)")
    void testFailPersistReviewWithInvalidUser() {

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


        try {
            jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                    "Esta review no debería insertarse", 5, itemId, fakeUserId);

            fail("Should have thrown an exception due to foreign key constraint violation");
        } catch (Exception e) {
            String errorMessage = e.getMessage().toLowerCase();
            assertTrue(errorMessage.contains("foreign key") ||
                      errorMessage.contains("referential integrity") ||
                      errorMessage.contains("constraint"),
                    "Should be a foreign key constraint violation error");
        }
    }

    /**
     * Test the execution of complex queries with JOINs to retrieve reviews along with user and item information
     */
    @Test
    @DisplayName("Should execute complex queries with JOINs to retrieve reviews with user and item information")
    void testComplexQueryWithJoins() {

        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        jdbcTemplate.update("INSERT INTO users (user_id_pk, username, password, created_at) VALUES (?, ?, ?, ?)",
                userId1, "critic1", "pass", timestamp);
        jdbcTemplate.update("INSERT INTO users (user_id_pk, username, password, created_at) VALUES (?, ?, ?, ?)",
                userId2, "critic2", "pass", timestamp);


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

        jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                "Mind-bending", 9, itemId1, userId1);
        jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                "Best TV show ever", 10, itemId2, userId1);
        jdbcTemplate.update("INSERT INTO reviews (review_text, score, item_id_fk, user_id_fk) VALUES (?, ?, ?, ?)",
                "Great movie", 8, itemId1, userId2);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT u.username, i.title, i.tmdb_type, r.review_text, r.score " +
                "FROM reviews r " +
                "INNER JOIN users u ON r.user_id_fk = u.user_id_pk " +
                "INNER JOIN items i ON r.item_id_fk = i.item_id_pk " +
                "ORDER BY r.score DESC"
        );

        
        assertNotNull(results);
        assertEquals(3, results.size(), "Should retrieve three reviews with JOINs");

        Map<String, Object> topReview = results.getFirst();
        assertEquals(10, topReview.get("score"));
        assertEquals("Best TV show ever", topReview.get("review_text"));
        assertEquals("Breaking Bad", topReview.get("title"));
    }

    /**
     * Test the update of an existing review.
     */
    @Test
    @DisplayName("Should update an existing review correctly")
    void testUpdateReview() {
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

        String updatedText = "Updated review after second viewing";
        int updatedScore = 9;

        jdbcTemplate.update(
                "UPDATE reviews SET review_text = ?, score = ? WHERE review_id_pk = ?",
                updatedText, updatedScore, reviewId
        );

        
        Map<String, Object> review = jdbcTemplate.queryForMap(
                "SELECT review_text, score FROM reviews WHERE review_id_pk = ?",
                reviewId
        );

        assertEquals(updatedText, review.get("review_text"));
        assertEquals(updatedScore, review.get("score"));
    }

    /**
     * Test the deletion of a review.
     */
    @Test
    @DisplayName("Should delete a review correctly and maintain data integrity")
    void testDeleteReview() {
        
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

        jdbcTemplate.update("DELETE FROM reviews WHERE review_id_pk = ?", reviewId);

        
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reviews WHERE review_id_pk = ?",
                Integer.class,
                reviewId
        );

        assertEquals(0, count, "The review should have been deleted");

        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE user_id_pk = ?",
                Integer.class,
                userId
        );
        assertEquals(1, userCount, "The user should still exist after review deletion");
    }
}

