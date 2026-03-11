package org.tvl.tvlooker.persistence.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.tvl.tvlooker.domain.model.entity.Interaction;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.entity.Review;
import org.tvl.tvlooker.domain.model.entity.User;
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
        User user = createAndSaveUser("user1");
        Item item = createAndSaveItem(1L, "Movie 1");

        Interaction interaction = Interaction.builder()
                .id(1L)
                .interactionType(InteractionType.VIEW)
                .user(user)
                .item(item)
                .build();

        // When
        Interaction savedInteraction = interactionRepository.save(interaction);

        // Then
        assertThat(savedInteraction).isNotNull();
        assertThat(savedInteraction.getId()).isEqualTo(1L);
        assertThat(savedInteraction.getInteractionType()).isEqualTo(InteractionType.VIEW);
    }

    @Test
    @DisplayName("Should save interaction with review")
    void testSaveInteractionWithReview() {
        // Given - Create user and item WITHOUT saving (Review has cascade persist on item)
        User user = createUser("user1");
        Item item = createItem(1L, "Movie 1");

        // Create and save Review - this will cascade persist user and item
        Review review = Review.builder()
                .reviewText("Great movie!")
                .score(8)
                .item(item)
                .user(user)
                .build();
        Review savedReview = reviewRepository.saveAndFlush(review);

        // Refresh to get managed entities
        User savedUser = userRepository.findById(user.getId()).orElseThrow();
        Item savedItem = itemRepository.findById(item.getId()).orElseThrow();

        Interaction interaction = Interaction.builder()
                .id(2L)
                .interactionType(InteractionType.REVIEW)
                .user(savedUser)
                .item(savedItem)
                .review(savedReview)
                .build();

        // When
        Interaction savedInteraction = interactionRepository.save(interaction);

        // Then
        assertThat(savedInteraction).isNotNull();
        assertThat(savedInteraction.getInteractionType()).isEqualTo(InteractionType.REVIEW);
        assertThat(savedInteraction.getReview()).isNotNull();
    }

    @Test
    @DisplayName("Should save multiple interactions")
    void testSaveMultipleInteractions() {
        // Given
        User user = createAndSaveUser("user1");
        Item item1 = createAndSaveItem(1L, "Movie 1");
        Item item2 = createAndSaveItem(2L, "Movie 2");

        Interaction interaction1 = Interaction.builder()
                .id(1L)
                .interactionType(InteractionType.VIEW)
                .user(user)
                .item(item1)
                .build();

        Interaction interaction2 = Interaction.builder()
                .id(2L)
                .interactionType(InteractionType.LIKE)
                .user(user)
                .item(item2)
                .build();

        // When
        List<Interaction> savedInteractions = interactionRepository.saveAll(List.of(interaction1, interaction2));

        // Then
        assertThat(savedInteractions).hasSize(2);
        assertThat(interactionRepository.count()).isEqualTo(2);
    }

    // ==================== READ/FIND TESTS ====================

    @Test
    @DisplayName("Should find interaction by ID")
    void testFindById() {
        // Given
        User user = createAndSaveUser("user1");
        Item item = createAndSaveItem(1L, "Movie 1");

        Interaction interaction = Interaction.builder()
                .id(1L)
                .interactionType(InteractionType.CLICK)
                .user(user)
                .item(item)
                .build();
        Interaction savedInteraction = interactionRepository.save(interaction);

        // When
        Optional<Interaction> foundInteraction = interactionRepository.findById(1L);

        // Then
        assertThat(foundInteraction).isPresent();
        assertThat(foundInteraction.get().getInteractionType()).isEqualTo(InteractionType.CLICK);
    }

    @Test
    @DisplayName("Should find all interactions")
    void testFindAll() {
        // Given
        User user = createAndSaveUser("user1");
        Item item1 = createAndSaveItem(1L, "Movie 1");
        Item item2 = createAndSaveItem(2L, "Movie 2");

        interactionRepository.saveAll(List.of(
                Interaction.builder().id(1L).interactionType(InteractionType.VIEW).user(user).item(item1).build(),
                Interaction.builder().id(2L).interactionType(InteractionType.LIKE).user(user).item(item2).build()
        ));

        // When
        List<Interaction> allInteractions = interactionRepository.findAll();

        // Then
        assertThat(allInteractions).hasSize(2);
    }

    @Test
    @DisplayName("Should count all interactions")
    void testCount() {
        // Given
        User user = createAndSaveUser("user1");
        Item item1 = createAndSaveItem(1L, "Movie 1");
        Item item2 = createAndSaveItem(2L, "Movie 2");

        interactionRepository.saveAll(List.of(
                Interaction.builder().id(1L).interactionType(InteractionType.VIEW).user(user).item(item1).build(),
                Interaction.builder().id(2L).interactionType(InteractionType.RATING).user(user).item(item2).build()
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
        User user1 = createAndSaveUser("user1");
        User user2 = createAndSaveUser("user2");
        Item item1 = createAndSaveItem(1L, "Movie 1");
        Item item2 = createAndSaveItem(2L, "Movie 2");
        Item item3 = createAndSaveItem(3L, "Movie 3");

        interactionRepository.saveAll(List.of(
                Interaction.builder().id(1L).interactionType(InteractionType.VIEW).user(user1).item(item1).build(),
                Interaction.builder().id(2L).interactionType(InteractionType.LIKE).user(user1).item(item2).build(),
                Interaction.builder().id(3L).interactionType(InteractionType.CLICK).user(user2).item(item3).build()
        ));

        // When
        List<Interaction> user1Interactions = interactionRepository.findByUser_Id(user1.getId());
        List<Interaction> user2Interactions = interactionRepository.findByUser_Id(user2.getId());

        // Then
        assertThat(user1Interactions).hasSize(2);
        assertThat(user2Interactions).hasSize(1);
    }

    @Test
    @DisplayName("Should return empty list when user has no interactions")
    void testFindByUserIdNoInteractions() {
        // Given
        User user = createAndSaveUser("user1");

        // When
        List<Interaction> interactions = interactionRepository.findByUser_Id(user.getId());

        // Then
        assertThat(interactions).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for non-existent user ID")
    void testFindByUserIdNotFound() {
        // Given
        UUID nonExistentUserId = UUID.randomUUID();

        // When
        List<Interaction> interactions = interactionRepository.findByUser_Id(nonExistentUserId);

        // Then
        assertThat(interactions).isEmpty();
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("Should update interaction type")
    void testUpdateInteraction() {
        // Given
        User user = createAndSaveUser("user1");
        Item item = createAndSaveItem(1L, "Movie 1");

        Interaction interaction = Interaction.builder()
                .id(1L)
                .interactionType(InteractionType.VIEW)
                .user(user)
                .item(item)
                .build();
        Interaction savedInteraction = interactionRepository.save(interaction);

        // When
        savedInteraction.setInteractionType(InteractionType.RESEARCH);
        Interaction updatedInteraction = interactionRepository.save(savedInteraction);

        // Then
        assertThat(updatedInteraction.getId()).isEqualTo(1L);
        assertThat(updatedInteraction.getInteractionType()).isEqualTo(InteractionType.RESEARCH);
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should delete interaction by ID")
    void testDeleteById() {
        // Given
        User user = createAndSaveUser("user1");
        Item item = createAndSaveItem(1L, "Movie 1");

        Interaction interaction = Interaction.builder()
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
        User user = createAndSaveUser("user1");
        Item item1 = createAndSaveItem(1L, "Movie 1");
        Item item2 = createAndSaveItem(2L, "Movie 2");

        interactionRepository.saveAll(List.of(
                Interaction.builder().id(1L).interactionType(InteractionType.VIEW).user(user).item(item1).build(),
                Interaction.builder().id(2L).interactionType(InteractionType.LIKE).user(user).item(item2).build()
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
        Item item = createAndSaveItem(1L, "Movie 1");

        Interaction interaction = Interaction.builder()
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
        User user = createAndSaveUser("user1");

        Interaction interaction = Interaction.builder()
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
        User user = createAndSaveUser("user1");
        Item item = createAndSaveItem(1L, "Movie 1");

        Interaction interaction = Interaction.builder()
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

    private User createAndSaveUser(String username) {
        User user = User.builder()
                .username(username)
                .password("password123")
                .build();
        return userRepository.saveAndFlush(user);
    }

    private Item createAndSaveItem(Long tmdbId, String title) {
        Item item = Item.builder()
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

    private User createUser(String username) {
        return User.builder()
                .username(username)
                .password("password123")
                .build();
    }

    private Item createItem(Long tmdbId, String title) {
        return Item.builder()
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
