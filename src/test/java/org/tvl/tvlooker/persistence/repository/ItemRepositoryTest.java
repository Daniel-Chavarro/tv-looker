package org.tvl.tvlooker.persistence.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.tvl.tvlooker.domain.model.entity.Actor;
import org.tvl.tvlooker.domain.model.entity.Director;
import org.tvl.tvlooker.domain.model.entity.Genre;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.enums.TmdbType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * TDD Test class for ItemRepository.
 * Tests cover CRUD operations, relationships, and constraints.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-11
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ItemRepository TDD Tests")
class ItemRepositoryTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private DirectorRepository directorRepository;

    @Autowired
    private ActorRepository actorRepository;

    @AfterEach
    void tearDown() {
        itemRepository.deleteAll();
        genreRepository.deleteAll();
        directorRepository.deleteAll();
        actorRepository.deleteAll();
    }

    // ==================== CREATE/SAVE TESTS ====================

    @Test
    @DisplayName("Should save a new movie item")
    void testSaveMovieItem() {
        // Given
        Item item = Item.builder()
                .tmdbId(12345L)
                .tmdbType(TmdbType.MOVIE)
                .title("The Matrix")
                .overview("A computer hacker learns about reality")
                .releaseDate(LocalDate.of(1999, 3, 31))
                .popularity(new BigDecimal("850.1234"))
                .voteAverage(new BigDecimal("8.71"))
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actors(new HashSet<>())
                .build();

        // When
        Item savedItem = itemRepository.saveAndFlush(item);

        // Then
        assertThat(savedItem).isNotNull();
        assertThat(savedItem.getId()).isNotNull();
        assertThat(savedItem.getTmdbId()).isEqualTo(12345L);
        assertThat(savedItem.getTmdbType()).isEqualTo(TmdbType.MOVIE);
        assertThat(savedItem.getTitle()).isEqualTo("The Matrix");
        assertThat(savedItem.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should save a new TV show item")
    void testSaveTvShowItem() {
        // Given
        Item item = Item.builder()
                .tmdbId(67890L)
                .tmdbType(TmdbType.TV)
                .title("Breaking Bad")
                .overview("A chemistry teacher turns to cooking meth")
                .releaseDate(LocalDate.of(2008, 1, 20))
                .popularity(new BigDecimal("950.5678"))
                .voteAverage(new BigDecimal("9.50"))
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actors(new HashSet<>())
                .build();

        // When
        Item savedItem = itemRepository.saveAndFlush(item);

        // Then
        assertThat(savedItem).isNotNull();
        assertThat(savedItem.getTmdbType()).isEqualTo(TmdbType.TV);
        assertThat(savedItem.getTitle()).isEqualTo("Breaking Bad");
    }

    @Test
    @DisplayName("Should save multiple items")
    void testSaveMultipleItems() {
        // Given
        Item item1 = createItem(1L, "Movie 1", TmdbType.MOVIE);
        Item item2 = createItem(2L, "Movie 2", TmdbType.MOVIE);
        Item item3 = createItem(3L, "TV Show 1", TmdbType.TV);

        // When
        List<Item> savedItems = itemRepository.saveAll(List.of(item1, item2, item3));

        // Then
        assertThat(savedItems).hasSize(3);
        assertThat(savedItems).extracting(Item::getTitle)
                .containsExactlyInAnyOrder("Movie 1", "Movie 2", "TV Show 1");
    }

    // ==================== READ/FIND TESTS ====================

    @Test
    @DisplayName("Should find item by ID")
    void testFindById() {
        // Given
        Item item = createItem(100L, "Findable Movie", TmdbType.MOVIE);
        Item savedItem = itemRepository.saveAndFlush(item);

        // When
        Optional<Item> foundItem = itemRepository.findById(savedItem.getId());

        // Then
        assertThat(foundItem).isPresent();
        assertThat(foundItem.get().getTitle()).isEqualTo("Findable Movie");
    }

    @Test
    @DisplayName("Should return empty when item ID not found")
    void testFindByIdNotFound() {
        // Given
        Long nonExistentId = 999999L;

        // When
        Optional<Item> foundItem = itemRepository.findById(nonExistentId);

        // Then
        assertThat(foundItem).isEmpty();
    }

    @Test
    @DisplayName("Should find all items")
    void testFindAll() {
        // Given
        itemRepository.saveAll(List.of(
                createItem(1L, "Item 1", TmdbType.MOVIE),
                createItem(2L, "Item 2", TmdbType.TV),
                createItem(3L, "Item 3", TmdbType.MOVIE)
        ));

        // When
        List<Item> allItems = itemRepository.findAll();

        // Then
        assertThat(allItems).hasSize(3);
    }

    @Test
    @DisplayName("Should check if item exists by ID")
    void testExistsById() {
        // Given
        Item item = createItem(200L, "Existing Item", TmdbType.MOVIE);
        Item savedItem = itemRepository.saveAndFlush(item);

        // When
        boolean exists = itemRepository.existsById(savedItem.getId());
        boolean notExists = itemRepository.existsById(999999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should count all items")
    void testCount() {
        // Given
        itemRepository.saveAll(List.of(
                createItem(1L, "Item 1", TmdbType.MOVIE),
                createItem(2L, "Item 2", TmdbType.TV)
        ));

        // When
        long count = itemRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("Should update existing item")
    void testUpdateItem() {
        // Given
        Item item = createItem(300L, "Original Title", TmdbType.MOVIE);
        Item savedItem = itemRepository.saveAndFlush(item);

        // When
        savedItem.setTitle("Updated Title");
        savedItem.setPopularity(new BigDecimal("999.9999"));
        Item updatedItem = itemRepository.saveAndFlush(savedItem);

        // Then
        assertThat(updatedItem.getId()).isEqualTo(savedItem.getId());
        assertThat(updatedItem.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedItem.getPopularity()).isEqualByComparingTo(new BigDecimal("999.9999"));
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should delete item by ID")
    void testDeleteById() {
        // Given
        Item item = createItem(400L, "Deletable Item", TmdbType.MOVIE);
        Item savedItem = itemRepository.saveAndFlush(item);

        // When
        itemRepository.deleteById(savedItem.getId());

        // Then
        assertThat(itemRepository.findById(savedItem.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should delete item entity")
    void testDelete() {
        // Given
        Item item = createItem(500L, "Delete Me", TmdbType.MOVIE);
        Item savedItem = itemRepository.saveAndFlush(item);

        // When
        itemRepository.delete(savedItem);

        // Then
        assertThat(itemRepository.findById(savedItem.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should delete all items")
    void testDeleteAll() {
        // Given
        itemRepository.saveAll(List.of(
                createItem(1L, "Item 1", TmdbType.MOVIE),
                createItem(2L, "Item 2", TmdbType.TV)
        ));

        // When
        itemRepository.deleteAll();

        // Then
        assertThat(itemRepository.count()).isZero();
    }

    // ==================== RELATIONSHIP TESTS ====================
    // Note: These tests demonstrate that items can be saved with relationships.
    // The CASCADE.PERSIST configuration requires careful entity state management.

    @Test
    @DisplayName("Should save item with genres using cascade persist")
    void testSaveItemWithGenres() {
        // Given - Create new genres (not persisted yet)
        Genre action = new Genre();
        action.setName("Action");
        Genre sciFi = new Genre();
        sciFi.setName("Sci-Fi");

        Set<Genre> genres = new HashSet<>(List.of(action, sciFi));

        Item item = Item.builder()
                .tmdbId(600L)
                .tmdbType(TmdbType.MOVIE)
                .title("Action Sci-Fi Movie")
                .genres(genres)
                .directors(new HashSet<>())
                .actors(new HashSet<>())
                .build();

        // When - Item save will cascade to genres
        Item savedItem = itemRepository.saveAndFlush(item);

        // Then
        assertThat(savedItem.getGenres()).hasSize(2);
        assertThat(savedItem.getGenres()).extracting(Genre::getName)
                .containsExactlyInAnyOrder("Action", "Sci-Fi");
        // Verify genres were persisted
        assertThat(genreRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should save item with directors using cascade persist")
    void testSaveItemWithDirectors() {
        // Given - Create new directors (not persisted yet)
        Director director1 = new Director();
        director1.setName("Christopher Nolan");
        director1.setTmdbId(1000L);
        Director director2 = new Director();
        director2.setName("Steven Spielberg");
        director2.setTmdbId(2000L);

        Set<Director> directors = new HashSet<>(List.of(director1, director2));

        Item item = Item.builder()
                .tmdbId(700L)
                .tmdbType(TmdbType.MOVIE)
                .title("Epic Movie")
                .directors(directors)
                .genres(new HashSet<>())
                .actors(new HashSet<>())
                .build();

        // When - Item save will cascade to directors
        Item savedItem = itemRepository.saveAndFlush(item);

        // Then
        assertThat(savedItem.getDirectors()).hasSize(2);
        assertThat(savedItem.getDirectors()).extracting(Director::getName)
                .containsExactlyInAnyOrder("Christopher Nolan", "Steven Spielberg");
        // Verify directors were persisted
        assertThat(directorRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should save item with actors using cascade persist")
    void testSaveItemWithActors() {
        // Given - Create new actors (not persisted yet)
        Actor actor1 = new Actor();
        actor1.setName("Leonardo DiCaprio");
        actor1.setTmdbId(3000L);
        Actor actor2 = new Actor();
        actor2.setName("Tom Hanks");
        actor2.setTmdbId(4000L);

        Set<Actor> actors = new HashSet<>(List.of(actor1, actor2));

        Item item = Item.builder()
                .tmdbId(800L)
                .tmdbType(TmdbType.MOVIE)
                .title("Star-Studded Film")
                .actors(actors)
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .build();

        // When - Item save will cascade to actors
        Item savedItem = itemRepository.saveAndFlush(item);

        // Then
        assertThat(savedItem.getActors()).hasSize(2);
        assertThat(savedItem.getActors()).extracting(Actor::getName)
                .containsExactlyInAnyOrder("Leonardo DiCaprio", "Tom Hanks");
        // Verify actors were persisted
        assertThat(actorRepository.count()).isEqualTo(2);
    }

    // ==================== CONSTRAINT TESTS ====================

    @Test
    @DisplayName("Should enforce tmdbId uniqueness constraint")
    void testTmdbIdUniqueConstraint() {
        // Given
        Item item1 = createItem(9999L, "First Movie", TmdbType.MOVIE);
        itemRepository.saveAndFlush(item1);

        Item item2 = createItem(9999L, "Duplicate TMDB ID", TmdbType.MOVIE);

        // When & Then
        try {
            itemRepository.saveAndFlush(item2);
            fail("Should have thrown exception for duplicate tmdbId");
        } catch (Exception e) {
            // Expected exception due to unique constraint violation
            assertThat(e.getMessage()).containsAnyOf("unique", "constraint", "duplicate", "Unique");
        }
    }

    @Test
    @DisplayName("Should not allow null tmdbId")
    void testNullTmdbId() {
        // Given
        Item item = Item.builder()
                .tmdbId(null)
                .tmdbType(TmdbType.MOVIE)
                .title("Invalid Item")
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actors(new HashSet<>())
                .build();

        // When & Then
        try {
            itemRepository.saveAndFlush(item);
            fail("Should have thrown exception for null tmdbId");
        } catch (Exception e) {
            // Expected exception due to null constraint violation
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should not allow null tmdbType")
    void testNullTmdbType() {
        // Given
        Item item = Item.builder()
                .tmdbId(10000L)
                .tmdbType(null)
                .title("Invalid Type Item")
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actors(new HashSet<>())
                .build();

        // When & Then
        try {
            itemRepository.saveAndFlush(item);
            fail("Should have thrown exception for null tmdbType");
        } catch (Exception e) {
            // Expected exception due to null constraint violation
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should not allow null title")
    void testNullTitle() {
        // Given
        Item item = Item.builder()
                .tmdbId(10001L)
                .tmdbType(TmdbType.MOVIE)
                .title(null)
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actors(new HashSet<>())
                .build();

        // When & Then
        try {
            itemRepository.saveAndFlush(item);
            fail("Should have thrown exception for null title");
        } catch (Exception e) {
            // Expected exception due to null constraint violation
            assertThat(e).isNotNull();
        }
    }

    // ==================== HELPER METHODS ====================

    private Item createItem(Long tmdbId, String title, TmdbType type) {
        return Item.builder()
                .tmdbId(tmdbId)
                .tmdbType(type)
                .title(title)
                .overview("Overview for " + title)
                .releaseDate(LocalDate.now())
                .popularity(new BigDecimal("100.0000"))
                .voteAverage(new BigDecimal("7.50"))
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actors(new HashSet<>())
                .build();
    }
}
