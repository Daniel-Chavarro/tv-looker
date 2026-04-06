package org.tvl.tvlooker.persistence.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.entity.ActorItemEntity;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
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
        ItemEntity item = ItemEntity.builder()
                .tmdbId(12345L)
                .tmdbType(TmdbType.MOVIE)
                .title("The Matrix")
                .overview("A computer hacker learns about reality")
                .releaseDate(LocalDate.of(1999, 3, 31))
                .popularity(new BigDecimal("850.1234"))
                .voteAverage(new BigDecimal("8.71"))
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actorItems(new HashSet<>())
                .build();

        // When
        ItemEntity savedItem = itemRepository.saveAndFlush(item);

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
        ItemEntity item = ItemEntity.builder()
                .tmdbId(67890L)
                .tmdbType(TmdbType.TV)
                .title("Breaking Bad")
                .overview("A chemistry teacher turns to cooking meth")
                .releaseDate(LocalDate.of(2008, 1, 20))
                .popularity(new BigDecimal("950.5678"))
                .voteAverage(new BigDecimal("9.50"))
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actorItems(new HashSet<>())
                .build();

        // When
        ItemEntity savedItem = itemRepository.saveAndFlush(item);

        // Then
        assertThat(savedItem).isNotNull();
        assertThat(savedItem.getTmdbType()).isEqualTo(TmdbType.TV);
        assertThat(savedItem.getTitle()).isEqualTo("Breaking Bad");
    }

    @Test
    @DisplayName("Should save multiple items")
    void testSaveMultipleItems() {
        // Given
        ItemEntity item1 = createItemEntity(1L, "Movie 1", TmdbType.MOVIE);
        ItemEntity item2 = createItemEntity(2L, "Movie 2", TmdbType.MOVIE);
        ItemEntity item3 = createItemEntity(3L, "TV Show 1", TmdbType.TV);

        // When
        List<ItemEntity> savedItems = itemRepository.saveAll(List.of(item1, item2, item3));

        // Then
        assertThat(savedItems).hasSize(3);
        assertThat(savedItems).extracting(ItemEntity::getTitle)
                .containsExactlyInAnyOrder("Movie 1", "Movie 2", "TV Show 1");
    }

    // ==================== READ/FIND TESTS ====================

    @Test
    @DisplayName("Should find item by ID")
    void testFindById() {
        // Given
        ItemEntity item = createItemEntity(100L, "Findable Movie", TmdbType.MOVIE);
        ItemEntity savedItem = itemRepository.saveAndFlush(item);

        // When
        Optional<ItemEntity> foundItem = itemRepository.findById(savedItem.getId());

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
        Optional<ItemEntity> foundItem = itemRepository.findById(nonExistentId);

        // Then
        assertThat(foundItem).isEmpty();
    }

    @Test
    @DisplayName("Should find all items")
    void testFindAll() {
        // Given
        itemRepository.saveAll(List.of(
                createItemEntity(1L, "ItemEntity 1", TmdbType.MOVIE),
                createItemEntity(2L, "ItemEntity 2", TmdbType.TV),
                createItemEntity(3L, "ItemEntity 3", TmdbType.MOVIE)
        ));

        // When
        List<ItemEntity> allItems = itemRepository.findAll();

        // Then
        assertThat(allItems).hasSize(3);
    }

    @Test
    @DisplayName("Should check if item exists by ID")
    void testExistsById() {
        // Given
        ItemEntity item = createItemEntity(200L, "Existing ItemEntity", TmdbType.MOVIE);
        ItemEntity savedItem = itemRepository.saveAndFlush(item);

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
                createItemEntity(1L, "ItemEntity 1", TmdbType.MOVIE),
                createItemEntity(2L, "ItemEntity 2", TmdbType.TV)
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
        ItemEntity item = createItemEntity(300L, "Original Title", TmdbType.MOVIE);
        ItemEntity savedItem = itemRepository.saveAndFlush(item);

        // When
        savedItem.setTitle("Updated Title");
        savedItem.setPopularity(new BigDecimal("999.9999"));
        ItemEntity updatedItem = itemRepository.saveAndFlush(savedItem);

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
        ItemEntity item = createItemEntity(400L, "Deletable ItemEntity", TmdbType.MOVIE);
        ItemEntity savedItem = itemRepository.saveAndFlush(item);

        // When
        itemRepository.deleteById(savedItem.getId());

        // Then
        assertThat(itemRepository.findById(savedItem.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should delete item entity")
    void testDelete() {
        // Given
        ItemEntity item = createItemEntity(500L, "Delete Me", TmdbType.MOVIE);
        ItemEntity savedItem = itemRepository.saveAndFlush(item);

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
                createItemEntity(1L, "ItemEntity 1", TmdbType.MOVIE),
                createItemEntity(2L, "ItemEntity 2", TmdbType.TV)
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
        GenreEntity action = new GenreEntity();
        action.setName("Action");
        GenreEntity sciFi = new GenreEntity();
        sciFi.setName("Sci-Fi");

        Set<GenreEntity> genres = new HashSet<>(List.of(action, sciFi));

        ItemEntity item = ItemEntity.builder()
                .tmdbId(600L)
                .tmdbType(TmdbType.MOVIE)
                .title("Action Sci-Fi Movie")
                .genres(genres)
                .directors(new HashSet<>())
                .actorItems(new HashSet<>())
                .build();

        // When - ItemEntity save will cascade to genres
        ItemEntity savedItem = itemRepository.saveAndFlush(item);

        // Then
        assertThat(savedItem.getGenres()).hasSize(2);
        assertThat(savedItem.getGenres()).extracting(GenreEntity::getName)
                .containsExactlyInAnyOrder("Action", "Sci-Fi");
        // Verify genres were persisted
        assertThat(genreRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should save item with directors using cascade persist")
    void testSaveItemWithDirectors() {
        // Given - Create new directors (not persisted yet)
        DirectorEntity director1 = new DirectorEntity();
        director1.setName("Christopher Nolan");
        director1.setTmdbId(1000L);
        DirectorEntity director2 = new DirectorEntity();
        director2.setName("Steven Spielberg");
        director2.setTmdbId(2000L);

        Set<DirectorEntity> directors = new HashSet<>(List.of(director1, director2));

        ItemEntity item = ItemEntity.builder()
                .tmdbId(700L)
                .tmdbType(TmdbType.MOVIE)
                .title("Epic Movie")
                .directors(directors)
                .genres(new HashSet<>())
                .actorItems(new HashSet<>())
                .build();

        // When - ItemEntity save will cascade to directors
        ItemEntity savedItem = itemRepository.saveAndFlush(item);

        // Then
        assertThat(savedItem.getDirectors()).hasSize(2);
        assertThat(savedItem.getDirectors()).extracting(DirectorEntity::getName)
                .containsExactlyInAnyOrder("Christopher Nolan", "Steven Spielberg");
        // Verify directors were persisted
        assertThat(directorRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should save item with actorItems using cascade persist")
    void testSaveItemWithActors() {
        // Given - Create new actorItems (not persisted yet)
        ActorEntity actor1 = new ActorEntity();
        actor1.setName("Leonardo DiCaprio");
        actor1.setTmdbId(3000L);
        ActorEntity actor2 = new ActorEntity();
        actor2.setName("Tom Hanks");
        actor2.setTmdbId(4000L);

        ActorItemEntity actorItem1 = ActorItemEntity.builder()
                .actor(actor1)
                .billingOrder(0)
                .build();
        ActorItemEntity actorItem2 = ActorItemEntity.builder()
                .actor(actor2)
                .billingOrder(1)
                .build();

        Set<ActorItemEntity> actorItems = new HashSet<>(List.of(actorItem1, actorItem2));

        ItemEntity item = ItemEntity.builder()
                .tmdbId(800L)
                .tmdbType(TmdbType.MOVIE)
                .title("Star-Studded Film")
                .actorItems(actorItems)
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .build();

        // When - ItemEntity save will cascade to actorItems
        ItemEntity savedItem = itemRepository.saveAndFlush(item);

        // Then
        assertThat(savedItem.getActorItems()).hasSize(2);
        assertThat(savedItem.getActorItems()).extracting(actorItem -> actorItem.getActor().getName())
                .containsExactlyInAnyOrder("Leonardo DiCaprio", "Tom Hanks");
        // Verify actorItems were persisted
        assertThat(actorRepository.count()).isEqualTo(2);
    }

    // ==================== CONSTRAINT TESTS ====================

    @Test
    @DisplayName("Should enforce tmdbId uniqueness constraint")
    void testTmdbIdUniqueConstraint() {
        // Given
        ItemEntity item1 = createItemEntity(9999L, "First Movie", TmdbType.MOVIE);
        itemRepository.saveAndFlush(item1);

        ItemEntity item2 = createItemEntity(9999L, "Duplicate TMDB ID", TmdbType.MOVIE);

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
        ItemEntity item = ItemEntity.builder()
                .tmdbId(null)
                .tmdbType(TmdbType.MOVIE)
                .title("Invalid ItemEntity")
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actorItems(new HashSet<>())
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
        ItemEntity item = ItemEntity.builder()
                .tmdbId(10000L)
                .tmdbType(null)
                .title("Invalid Type ItemEntity")
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actorItems(new HashSet<>())
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
        ItemEntity item = ItemEntity.builder()
                .tmdbId(10001L)
                .tmdbType(TmdbType.MOVIE)
                .title(null)
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actorItems(new HashSet<>())
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

    private ItemEntity createItemEntity(Long tmdbId, String title, TmdbType type) {
        return ItemEntity.builder()
                .tmdbId(tmdbId)
                .tmdbType(type)
                .title(title)
                .overview("Overview for " + title)
                .releaseDate(LocalDate.now())
                .popularity(new BigDecimal("100.0000"))
                .voteAverage(new BigDecimal("7.50"))
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actorItems(new HashSet<>())
                .build();
    }
}
