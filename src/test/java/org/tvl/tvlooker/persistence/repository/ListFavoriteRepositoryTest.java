package org.tvl.tvlooker.persistence.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.model.entity.ListFavorite;
import org.tvl.tvlooker.domain.model.entity.User;
import org.tvl.tvlooker.domain.model.enums.TmdbType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * TDD Test class for ListFavoriteRepository.
 * Tests cover CRUD operations, custom queries, and relationships.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-11
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ListFavoriteRepository TDD Tests")
class ListFavoriteRepositoryTest {

    @Autowired
    private ListFavoriteRepository listFavoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @AfterEach
    void tearDown() {
        listFavoriteRepository.deleteAll();
        itemRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== CREATE/SAVE TESTS ====================

    @Test
    @DisplayName("Should save a new list favorite")
    void testSaveListFavorite() {
        // Given - Create user and items WITHOUT saving (ListFavorite has cascade persist)
        User user = createUser("user1");
        Item item1 = createItem(1L, "Movie 1");
        Item item2 = createItem(2L, "Movie 2");
        Set<Item> items = new HashSet<>();
        items.add(item1);
        items.add(item2);

        ListFavorite listFavorite = ListFavorite.builder()
                .name("My Favorites")
                .description("My favorite movies")
                .user(user)
                .items(items)
                .build();

        // When - Save list (cascade will persist user and items)
        ListFavorite savedList = listFavoriteRepository.saveAndFlush(listFavorite);

        // Then
        assertThat(savedList).isNotNull();
        assertThat(savedList.getId()).isNotNull();
        assertThat(savedList.getName()).isEqualTo("My Favorites");
        assertThat(savedList.getDescription()).isEqualTo("My favorite movies");
        assertThat(savedList.getUser()).isNotNull();
        assertThat(savedList.getItems()).hasSize(2);
    }

    @Test
    @DisplayName("Should save list without description")
    void testSaveListWithoutDescription() {
        // Given
        User user = createUser("user1");

        ListFavorite listFavorite = ListFavorite.builder()
                .name("Favorites List")
                .description(null)
                .user(user)
                .items(new HashSet<>())
                .build();

        // When
        ListFavorite savedList = listFavoriteRepository.saveAndFlush(listFavorite);

        // Then
        assertThat(savedList).isNotNull();
        assertThat(savedList.getDescription()).isNull();
    }

    @Test
    @DisplayName("Should save multiple lists for different users")
    void testSaveMultipleLists() {
        // Given
        User user1 = createUser("user1");
        User user2 = createUser("user2");

        ListFavorite list1 = ListFavorite.builder()
                .name("User1 Favorites")
                .user(user1)
                .items(new HashSet<>())
                .build();

        ListFavorite list2 = ListFavorite.builder()
                .name("User2 Favorites")
                .user(user2)
                .items(new HashSet<>())
                .build();

        // When
        List<ListFavorite> savedLists = listFavoriteRepository.saveAll(List.of(list1, list2));

        // Then
        assertThat(savedLists).hasSize(2);
        assertThat(listFavoriteRepository.count()).isEqualTo(2);
    }

    // ==================== READ/FIND TESTS ====================

    @Test
    @DisplayName("Should find list by ID")
    void testFindById() {
        // Given
        User user = createUser("user1");
        Item item = createItem(1L, "Movie 1");
        Set<Item> items = new HashSet<>();
        items.add(item);

        ListFavorite listFavorite = ListFavorite.builder()
                .name("Findable List")
                .user(user)
                .items(items)
                .build();
        ListFavorite savedList = listFavoriteRepository.saveAndFlush(listFavorite);

        // When
        Optional<ListFavorite> foundList = listFavoriteRepository.findById(savedList.getId());

        // Then
        assertThat(foundList).isPresent();
        assertThat(foundList.get().getName()).isEqualTo("Findable List");
    }

    @Test
    @DisplayName("Should find all lists")
    void testFindAll() {
        // Given
        User user = createUser("user1");

        listFavoriteRepository.saveAll(List.of(
                ListFavorite.builder().name("List 1").user(user).items(new HashSet<>()).build(),
                ListFavorite.builder().name("List 2").user(user).items(new HashSet<>()).build()
        ));

        // When
        List<ListFavorite> allLists = listFavoriteRepository.findAll();

        // Then
        assertThat(allLists).hasSize(2);
    }

    @Test
    @DisplayName("Should count all lists")
    void testCount() {
        // Given
        User user = createUser("user1");

        listFavoriteRepository.saveAll(List.of(
                ListFavorite.builder().name("List 1").user(user).items(new HashSet<>()).build(),
                ListFavorite.builder().name("List 2").user(user).items(new HashSet<>()).build()
        ));

        // When
        long count = listFavoriteRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }

    // ==================== CUSTOM QUERY TESTS ====================

    @Test
    @DisplayName("Should find lists by user ID")
    void testFindByUserId() {
        // Given
        User user1 = createUser("user1");
        User user2 = createUser("user2");

        listFavoriteRepository.saveAll(List.of(
                ListFavorite.builder().name("User1 List 1").user(user1).items(new HashSet<>()).build(),
                ListFavorite.builder().name("User1 List 2").user(user1).items(new HashSet<>()).build(),
                ListFavorite.builder().name("User2 List 1").user(user2).items(new HashSet<>()).build()
        ));

        // When
        List<ListFavorite> user1Lists = listFavoriteRepository.findByUser_Id(user1.getId());
        List<ListFavorite> user2Lists = listFavoriteRepository.findByUser_Id(user2.getId());

        // Then
        assertThat(user1Lists).hasSize(2);
        assertThat(user2Lists).hasSize(1);
    }

    @Test
    @DisplayName("Should return empty list when user has no lists")
    void testFindByUserIdNoLists() {
        // Given
        User user = createUser("user1");
        userRepository.saveAndFlush(user);

        // When
        List<ListFavorite> lists = listFavoriteRepository.findByUser_Id(user.getId());

        // Then
        assertThat(lists).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list for non-existent user ID")
    void testFindByUserIdNotFound() {
        // Given
        UUID nonExistentUserId = UUID.randomUUID();

        // When
        List<ListFavorite> lists = listFavoriteRepository.findByUser_Id(nonExistentUserId);

        // Then
        assertThat(lists).isEmpty();
    }

    // ==================== UPDATE TESTS ====================

    @Test
    @DisplayName("Should update list name and description")
    void testUpdateList() {
        // Given
        User user = createUser("user1");

        ListFavorite listFavorite = ListFavorite.builder()
                .name("Original Name")
                .description("Original Description")
                .user(user)
                .items(new HashSet<>())
                .build();
        ListFavorite savedList = listFavoriteRepository.saveAndFlush(listFavorite);

        // When
        savedList.setName("Updated Name");
        savedList.setDescription("Updated Description");
        ListFavorite updatedList = listFavoriteRepository.saveAndFlush(savedList);

        // Then
        assertThat(updatedList.getId()).isEqualTo(savedList.getId());
        assertThat(updatedList.getName()).isEqualTo("Updated Name");
        assertThat(updatedList.getDescription()).isEqualTo("Updated Description");
    }

    // ==================== DELETE TESTS ====================

    @Test
    @DisplayName("Should delete list by ID")
    void testDeleteById() {
        // Given
        User user = createUser("user1");

        ListFavorite listFavorite = ListFavorite.builder()
                .name("Delete Me")
                .user(user)
                .items(new HashSet<>())
                .build();
        ListFavorite savedList = listFavoriteRepository.saveAndFlush(listFavorite);

        // When
        listFavoriteRepository.deleteById(savedList.getId());

        // Then
        assertThat(listFavoriteRepository.findById(savedList.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should delete all lists")
    void testDeleteAll() {
        // Given
        User user = createUser("user1");

        listFavoriteRepository.saveAll(List.of(
                ListFavorite.builder().name("List 1").user(user).items(new HashSet<>()).build(),
                ListFavorite.builder().name("List 2").user(user).items(new HashSet<>()).build()
        ));

        // When
        listFavoriteRepository.deleteAll();

        // Then
        assertThat(listFavoriteRepository.count()).isZero();
    }

    // ==================== CONSTRAINT TESTS ====================

    @Test
    @DisplayName("Should not allow null name")
    void testNullName() {
        // Given
        User user = createUser("user1");

        ListFavorite listFavorite = ListFavorite.builder()
                .name(null)
                .user(user)
                .items(new HashSet<>())
                .build();

        // When & Then
        try {
            listFavoriteRepository.saveAndFlush(listFavorite);
            fail("Should have thrown exception for null name");
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should not allow null user")
    void testNullUser() {
        // Given
        ListFavorite listFavorite = ListFavorite.builder()
                .name("Test List")
                .user(null)
                .items(new HashSet<>())
                .build();

        // When & Then
        try {
            listFavoriteRepository.saveAndFlush(listFavorite);
            fail("Should have thrown exception for null user");
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("Should enforce unique name constraint")
    void testUniqueName() {
        // Given
        User user = createUser("user1");

        ListFavorite list1 = ListFavorite.builder()
                .name("Unique Name")
                .user(user)
                .items(new HashSet<>())
                .build();
        listFavoriteRepository.saveAndFlush(list1);

        User user2 = createUser("user2");
        ListFavorite list2 = ListFavorite.builder()
                .name("Unique Name")
                .user(user2)
                .items(new HashSet<>())
                .build();

        // When & Then
        try {
            listFavoriteRepository.saveAndFlush(list2);
            fail("Should have thrown exception for duplicate name");
        } catch (Exception e) {
            assertThat(e).isNotNull();
        }
    }

    // ==================== HELPER METHODS ====================

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
