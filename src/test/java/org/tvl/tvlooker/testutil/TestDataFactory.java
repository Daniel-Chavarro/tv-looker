package org.tvl.tvlooker.testutil;

import org.tvl.tvlooker.domain.model.entity.*;
import org.tvl.tvlooker.domain.model.enums.InteractionType;
import org.tvl.tvlooker.domain.model.enums.TmdbType;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;
import org.tvl.tvlooker.domain.data_structure.ScoredItem;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

/**
 * Factory class for creating test data fixtures.
 * Provides methods to create Users, Items, Interactions, and RecommendationContexts for testing.
 */
public class TestDataFactory {

    // ===================== User Factory Methods =====================

    /**
     * Creates a test user with default values.
     */
    public static User createUser(String username) {
        return User.builder()
                .id(UUID.randomUUID())
                .username(username)
                .password("password123")
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();
    }

    /**
     * Creates a test user with a specific ID.
     */
    public static User createUser(UUID id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .password("password123")
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();
    }

    /**
     * Creates multiple test users.
     */
    public static List<User> createUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            users.add(createUser("user" + i));
        }
        return users;
    }

    // ===================== Item Factory Methods =====================

    /**
     * Creates a test item with default values.
     */
    public static Item createItem(Long id, String title, double popularity) {
        return Item.builder()
                .id(id)
                .tmdbId(id * 1000)
                .tmdbType(TmdbType.MOVIE)
                .title(title)
                .overview("Overview for " + title)
                .releaseDate(LocalDate.now().minusYears(1))
                .popularity(BigDecimal.valueOf(popularity))
                .voteAverage(BigDecimal.valueOf(7.5))
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actors(new HashSet<>())
                .build();
    }

    /**
     * Creates a movie with specific attributes.
     */
    public static Item createMovie(Long id, String title, double popularity, double voteAverage) {
        return Item.builder()
                .id(id)
                .tmdbId(id * 1000)
                .tmdbType(TmdbType.MOVIE)
                .title(title)
                .overview("Overview for " + title)
                .releaseDate(LocalDate.now().minusYears(1))
                .popularity(BigDecimal.valueOf(popularity))
                .voteAverage(BigDecimal.valueOf(voteAverage))
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actors(new HashSet<>())
                .build();
    }

    /**
     * Creates a TV show with specific attributes.
     */
    public static Item createTvShow(Long id, String title, double popularity, double voteAverage) {
        return Item.builder()
                .id(id)
                .tmdbId(id * 1000)
                .tmdbType(TmdbType.TV)
                .title(title)
                .overview("Overview for " + title)
                .releaseDate(LocalDate.now().minusYears(1))
                .popularity(BigDecimal.valueOf(popularity))
                .voteAverage(BigDecimal.valueOf(voteAverage))
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .genres(new HashSet<>())
                .directors(new HashSet<>())
                .actors(new HashSet<>())
                .build();
    }

    /**
     * Creates multiple items with varying popularity.
     */
    public static List<Item> createItems(int count) {
        List<Item> items = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            // Create items with varying popularity (100 to 900)
            double popularity = 100.0 + (i * 100.0);
            items.add(createItem((long) i, "Item " + i, popularity));
        }
        return items;
    }

    /**
     * Creates items with specific popularity values for testing normalization.
     */
    public static List<Item> createItemsWithPopularity(Map<Long, Double> popularityMap) {
        List<Item> items = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : popularityMap.entrySet()) {
            items.add(createItem(entry.getKey(), "Item " + entry.getKey(), entry.getValue()));
        }
        return items;
    }

    // ===================== Genre Factory Methods =====================

    /**
     * Creates a test genre.
     */
    public static Genre createGenre(Long id, String name) {
        Genre genre = new Genre();
        genre.setId(id);
        genre.setName(name);
        return genre;
    }

    // ===================== Interaction Factory Methods =====================

    /**
     * Creates a test interaction.
     */
    public static Interaction createInteraction(Long id, User user, Item item, InteractionType type) {
        return Interaction.builder()
                .id(id)
                .user(user)
                .item(item)
                .interactionType(type)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();
    }

    /**
     * Creates a watch interaction.
     */
    public static Interaction createWatchInteraction(Long id, User user, Item item) {
        return createInteraction(id, user, item, InteractionType.VIEW);
    }

    /**
     * Creates multiple interactions for a user.
     */
    public static List<Interaction> createInteractionsForUser(User user, List<Item> items) {
        List<Interaction> interactions = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            interactions.add(createWatchInteraction((long) i, user, items.get(i)));
        }
        return interactions;
    }

    /**
     * Creates interactions for multiple users watching different items.
     */
    public static List<Interaction> createInteractions(List<User> users, List<Item> items) {
        List<Interaction> interactions = new ArrayList<>();
        long interactionId = 1L;
        
        for (User user : users) {
            // Each user watches a subset of items
            int itemsToWatch = Math.min(3, items.size());
            for (int i = 0; i < itemsToWatch; i++) {
                interactions.add(createWatchInteraction(interactionId++, user, items.get(i)));
            }
        }
        return interactions;
    }

    // ===================== ScoredItem Factory Methods =====================

    /**
     * Creates a scored item.
     */
    public static ScoredItem createScoredItem(Item item, double score, String explanation, String strategy) {
        return ScoredItem.builder()
                .item(item)
                .score(score)
                .explanation(explanation)
                .sourceStrategy(strategy)
                .build();
    }

    /**
     * Creates multiple scored items.
     */
    public static List<ScoredItem> createScoredItems(List<Item> items, String strategy) {
        List<ScoredItem> scoredItems = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            double score = 1.0 - (i * 0.1); // Decreasing scores
            scoredItems.add(createScoredItem(
                    items.get(i),
                    score,
                    "Recommendation from " + strategy,
                    strategy
            ));
        }
        return scoredItems;
    }

    // ===================== RecommendationContext Factory Methods =====================

    /**
     * Creates a basic recommendation context with users, items, and interactions.
     */
    public static RecommendationContext createContext(
            List<User> users,
            List<Item> items,
            List<Interaction> interactions) {
        return RecommendationContext.builder()
                .users(users)
                .items(items)
                .interactions(interactions)
                .dataProviders(new HashMap<>())
                .dataCache(new HashMap<>())
                .build();
    }

    /**
     * Creates a context with no interactions (cold start scenario).
     */
    public static RecommendationContext createColdStartContext(List<User> users, List<Item> items) {
        return createContext(users, items, new ArrayList<>());
    }

    /**
     * Creates a fully populated test context with multiple users, items, and interactions.
     */
    public static RecommendationContext createPopulatedContext() {
        List<User> users = createUsers(5);
        List<Item> items = createItems(10);
        List<Interaction> interactions = createInteractions(users, items);
        return createContext(users, items, interactions);
    }

    /**
     * Creates a minimal context for a single user with few items (new user scenario).
     */
    public static RecommendationContext createNewUserContext(User user) {
        List<User> users = List.of(user);
        List<Item> items = createItems(5);
        List<Interaction> interactions = new ArrayList<>(); // No interactions yet
        return createContext(users, items, interactions);
    }

    /**
     * Creates a context for an experienced user with many interactions.
     */
    public static RecommendationContext createExperiencedUserContext(User user) {
        List<User> users = List.of(user);
        List<Item> items = createItems(20);
        // User has watched 15 items
        List<Interaction> interactions = createInteractionsForUser(user, items.subList(0, 15));
        return createContext(users, items, interactions);
    }

    // ===================== Scenario-Specific Factory Methods =====================

    /**
     * Creates a context with empty items list (edge case).
     */
    public static RecommendationContext createEmptyItemsContext() {
        List<User> users = createUsers(1);
        return createContext(users, new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Creates a context with empty users list (edge case).
     */
    public static RecommendationContext createEmptyUsersContext() {
        List<Item> items = createItems(5);
        return createContext(new ArrayList<>(), items, new ArrayList<>());
    }

    /**
     * Creates a context with all null data (invalid scenario for testing validation).
     */
    public static RecommendationContext createNullDataContext() {
        return RecommendationContext.builder()
                .users(null)
                .items(null)
                .interactions(null)
                .dataProviders(new HashMap<>())
                .dataCache(new HashMap<>())
                .build();
    }

    /**
     * Creates items with known popularity for testing normalization.
     * Returns items with popularity: 100, 200, 400, 800 (max)
     */
    public static List<Item> createItemsForNormalizationTest() {
        Map<Long, Double> popularityMap = new LinkedHashMap<>();
        popularityMap.put(1L, 100.0);
        popularityMap.put(2L, 200.0);
        popularityMap.put(3L, 400.0);
        popularityMap.put(4L, 800.0); // Max popularity
        return createItemsWithPopularity(popularityMap);
    }
}
