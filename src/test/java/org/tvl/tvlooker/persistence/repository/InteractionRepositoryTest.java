package org.tvl.tvlooker.persistence.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.tvl.tvlooker.domain.model.entity.InteractionEntity;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.entity.ReviewEntity;
import org.tvl.tvlooker.domain.model.entity.UserEntity;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.model.enums.TmdbType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Test class for InteractionRepository.
 * Tests cover CRUD operations, custom queries, and relationships.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-11
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("InteractionRepository TDD Tests")
class InteractionRepositoryTest {

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @AfterEach
    void tearDown() {
        interactionRepository.deleteAll();
        reviewRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== CREATE/SAVE TESTS ====================

    @Test
    @DisplayName("Should save a new interaction")
    void testSaveInteraction() {
        // Given
        UserEntity user = createAndSaveUserEntity("user1");
        ItemEntity item = createAndSaveItemEntity(1L, "Movie 1");

        InteractionEntity interaction = InteractionEntity.builder()
                .id(1L)
                .interactionType(InteractionType.VIEW)
                .user(user)
                .item(item)
                .build();

        // When
        InteractionEntity savedInteraction = interactionRepository.save(interaction);

        // Then
        assertThat(savedInteraction).isNotNull();
        assertThat(savedInteraction.getId()).isEqualTo(1L);
        assertThat(savedInteraction.getInteractionType()).isEqualTo(InteractionType.VIEW);
    }

    @Test
    @DisplayName("Should save interaction with review")
    void testSaveInteractionWithReview() {
        // Given - Create user and item WITHOUT saving (ReviewEntity has cascade persist on item)
        UserEntity user = createUserEntity("user1");
        ItemEntity item = createItemEntity(1L, "Movie 1");

        // Create and save ReviewEntity - this will cascade persist user and item
        ReviewEntity review = ReviewEntity.builder()
                .reviewText("Great movie!")
                .score(8)
                .item(item)
                .user(user)
                .build();
        ReviewEntity savedReview = reviewRepository.saveAndFlush(review);

        // Refresh to get managed entities
        UserEntity savedUser = userRepository.findById(user.getId()).orElseThrow();
        ItemEntity savedItem = itemRepository.findById(item.getId()).orElseThrow();

        InteractionEntity interaction = InteractionEntity.builder()
                .id(2L)
                .interactionType(InteractionType.REVIEW)
                .user(savedUser)
                .item(savedItem)
                .review(savedReview)
                .build();

        // When
        InteractionEntity savedInteraction = interactionRepository.save(interaction);

        // Then
        assertThat(savedInteraction).isNotNull();
        assertThat(savedInteraction.getInteractionType()).isEqualTo(InteractionType.REVIEW);
        assertThat(savedInteraction.getReview()).isNotNull();
    }

    @Test
    @DisplayName("Should save multiple interactions")
    void testSaveMultipleInteractions() {
        // Given
        UserEntity user = createAndSaveUserEntity("user1");
        ItemEntity item1 = createAndSaveItemEntity(1L, "Movie 1");
        ItemEntity item2 = createAndSaveItemEntity(2L, "Movie 2");

        InteractionEntity interaction1 = InteractionEntity.builder()
                .id(1L)
                .interactionType(InteractionType.VIEW)
                .user(user)
                .item(item1)
                .build();

        InteractionEntity interaction2 = InteractionEntity.builder()
                .id(2L)
                .interactionType(InteractionType.LIKE)
                .user(user)
                .item(item2)
                .build();

        // When
        List<InteractionEntity> savedInteractions = interactionRepository.saveAll(List.of(interaction1, interaction2));

        // Then
        assertThat(savedInteractions).hasSize(2);
        assertThat(interactionRepository.count()).isEqualTo(2);
    }

    // ==================== READ/FIND TESTS ====================

    @Test
    @DisplayName("Should find interaction by ID")
    void testFindById() {
        // Given
        UserEntity user = createAndSaveUserEntity("user1");
        ItemEntity item = createAndSaveItemEntity(1L, "Movie 1");

        InteractionEntity interaction = InteractionEntity.builder()
                .id(1L)
                .interactionType(InteractionType.CLICK)
                .user(user)
                .item(item)
                .build();
        InteractionEntity savedInteraction = interactionRepository.save(interaction);

        // When
        Optional<InteractionEntity> foundInteraction = interactionRepository.findById(1L);

        // Then
        assertThat(foundInteraction).isPresent();
        assertThat(foundInteraction.get().getInteractionType()).isEqualTo(InteractionType.CLICK);
    }

    @Test
    @DisplayName("Should find all interactions")
    void testFindAll() {
        // Given
        UserEntity user = createAndSaveUserEntity("user1");
        ItemEntity item1 = createAndSaveItemEntity(1L, "Movie 1");
        ItemEntity item2 = createAndSaveItemEntity(2L, "Movie 2");

        interactionRepository.saveAll(List.of(
                InteractionEntity.builder().id(1L).interactionType(InteractionType.VIEW).user(user).item(item1).build(),
                InteractionEntity.builder().id(2L).interactionType(InteractionType.LIKE).user(user).item(item2).build()
        ));

        // When
        List<InteractionEntity> allInteractions = interactionRepository.findAll();

        // Then
        assertThat(allInteractions).hasSize(2);
    }

    @Test
    @DisplayName("Should count all interactions")
    void testCount() {
        // Given
        UserEntity user = createAndSaveUserEntity("user1");
        ItemEntity item1 = createAndSaveItemEntity(1L, "Movie 1");
        ItemEntity item2 = createAndSaveItemEntity(2L, "Movie 2");

        interactionRepository.saveAll(List.of(
                InteractionEntity.builder().id(1L).interactionType(InteractionType.VIEW).user(user).item(item1).build(),
                InteractionEntity.builder().id(2L).interactionType(InteractionType.RATING).user(user).item(item2).build()
        ));

        // When
        long count = interactionRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    // ==================== CUSTOM QUERY TESTS ====================

    @Test
    @DisplayName("Should find interactions by user ID")
    void testFindByUserId() {
        // Given
        UserEntity user1 = createAndSaveUserEntity("user1");
        UserEntity user2 = createAndSaveUserEntity("user2");
        ItemEntity item1 = createAndSaveItemEntity(1L, "Movie 1");
        ItemEntity item2 = createAndSaveItemEntity(2L, "Movie 2");
        ItemEntity item3 = createAndSaveItemEntity(3L, "Movie 3");

        interactionRepository.saveAll(List.of(
                InteractionEntity.builder().id(1L).interactionType(InteractionType.VIEW).user(user1).item(item1).build(),
                InteractionEntity.builder().id(2L).interactionType(InteractionType.LIKE).user(user1).item(item2).build(),
                InteractionEntity.builder().id(3L).interactionType(InteractionType.CLICK).user(user2).item(item3).build()
        ));

        // When
        List<InteractionEntity> user1Interactions = interactionRepository.findByUserId(user1.getId());
        List<InteractionEntity> user2Interactions = interactionRepository.findByUserId(user2.getId());

        // Then
        assertThat(user1Interactions).hasSize(2);
        assertThat(user2Interactions).hasSize(1);
    }

    @Test
    @DisplayName("Should return empty list when user has no interactions")
    void testFindByUserIdNoInteractions() {
        // Given
        UserEntity user = createAndSaveUserEntity("user1");

        // When
        List<InteractionEntity> interactions = interactionRepository.findByUserId(user.getId());

        // Then
        assertThat(interactions).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for non-existent user ID")
    void testFindByUserIdNotFound() {
        // Given
        UUID nonExistentUserId = UUID.randomUUID();

        // When
        List<InteractionEntity> interactions = interactionRepository.findByUserId(nonExistentUserId);

        // Then
        assertThat(interactions).isEmpty();
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("Should update interaction type")
    void testUpdateInteraction() {
        // Given
        UserEntity user = createAndSaveUserEntity("user1");
        ItemEntity item = createAndSaveItemEntity(1L, "Movie 1");

        InteractionEntity interaction = InteractionEntity.builder()
                .id(1L)
                .interactionType(InteractionType.VIEW)
                .user(user)
                .item(item)
                .build();
        InteractionEntity savedInteraction = interactionRepository.save(interaction);

        // When
        savedInteraction.setInteractionType(InteractionType.RESEARCH);
        InteractionEntity updatedInteraction = interactionRepository.save(savedInteraction);

        // Then
        assertThat(updatedInteraction.getId()).isEqualTo(1L);
        assertThat(updatedInteraction.getInteractionType()).isEqualTo(InteractionType.RESEARCH);
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should delete interaction by ID")
    void testDeleteById() {
        // Given
        UserEntity user = createAndSaveUserEntity("user1");
        ItemEntity item = createAndSaveItemEntity(1L, "Movie 1");

        InteractionEntity interaction = InteractionEntity.builder()
                .id(1L)
                .interactionType(InteractionType.VIEW)
                .user(user)
                .item(item)
                .build();
        interactionRepository.save(interaction);

        // When
        interactionRepository.deleteById(1L);

        // Then
        assertThat(interactionRepository.findById(1L)).isEmpty();
    }

    @Test
    @DisplayName("Should delete all interactions")
    void testDeleteAll() {
        // Given
        UserEntity user = createAndSaveUserEntity("user1");
        ItemEntity item1 = createAndSaveItemEntity(1L, "Movie 1");
        ItemEntity item2 = createAndSaveItemEntity(2L, "Movie 2");

        interactionRepository.saveAll(List.of(
                InteractionEntity.builder().id(1L).interactionType(InteractionType.VIEW).user(user).item(item1).build(),
                InteractionEntity.builder().id(2L).interactionType(InteractionType.LIKE).user(user).item(item2).build()
        ));

        // When
        interactionRepository.deleteAll();

        // Then
        assertThat(interactionRepository.count()).isZero();
    }

    // ==================== CONSTRAINT TESTS ====================

    @Test
    @DisplayName("Should not allow null user")
    void testNullUser() {
        // Given
        ItemEntity item = createAndSaveItemEntity(1L, "Movie 1");

        InteractionEntity interaction = InteractionEntity.builder()
                .id(1L)
                .interactionType(InteractionType.VIEW)
                .user(null)
                .item(item)
                .build();

        // When & Then
        try {
            interactionRepository.save(interaction);
            Assertions.fail("Should have thrown exception for null user");
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should not allow null item")
    void testNullItem() {
        // Given
        UserEntity user = createAndSaveUserEntity("user1");

        InteractionEntity interaction = InteractionEntity.builder()
                .id(1L)
                .interactionType(InteractionType.VIEW)
                .user(user)
                .item(null)
                .build();

        // When & Then
        try {
            interactionRepository.save(interaction);
            Assertions.fail("Should have thrown exception for null item");
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should not allow null interaction type")
    void testNullInteractionType() {
        // Given
        UserEntity user = createAndSaveUserEntity("user1");
        ItemEntity item = createAndSaveItemEntity(1L, "Movie 1");

        InteractionEntity interaction = InteractionEntity.builder()
                .id(1L)
                .interactionType(null)
                .user(user)
                .item(item)
                .build();

        // When & Then
        try {
            interactionRepository.save(interaction);
            Assertions.fail("Should have thrown exception for null interaction type");
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    // ==================== HELPER METHODS ====================

    private UserEntity createAndSaveUserEntity(String username) {
        UserEntity user = UserEntity.builder()
                .username(username)
                .password("password123")
                .email(username + "@example.com")
                .build();
        return userRepository.saveAndFlush(user);
    }

    private ItemEntity createAndSaveItemEntity(Long tmdbId, String title) {
        ItemEntity item = ItemEntity.builder()
                .tmdbId(tmdbId)
                .tmdbType(TmdbType.MOVIE)
                .title(title)
                .overview("Overview for " + title)
                .releaseDate(LocalDate.now())
                .popularity(new BigDecimal("100.0000"))
                .voteAverage(new BigDecimal("7.50"))
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actors(new HashSet<>())
                .build();
        return itemRepository.saveAndFlush(item);
    }

    private UserEntity createUserEntity(String username) {
        return UserEntity.builder()
                .username(username)
                .password("password123")
                .email(username + "@example.com")
                .build();
    }

    private ItemEntity createItemEntity(Long tmdbId, String title) {
        return ItemEntity.builder()
                .tmdbId(tmdbId)
                .tmdbType(TmdbType.MOVIE)
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
