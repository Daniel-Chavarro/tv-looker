# Recommendation Strategies and Aggregation Methods Design

**Date:** 2026-03-06  
**Status:** Approved  
**Related Design:** [Recommendation Engine Architecture](2026-03-05-recommendation-engine-design.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Implementation Strategy](#implementation-strategy)
3. [Data Provider Architecture](#data-provider-architecture)
4. [Recommendation Strategies](#recommendation-strategies)
5. [Aggregation Strategies](#aggregation-strategies)
6. [Shared Utility Components](#shared-utility-components)
7. [Configuration & Spring Integration](#configuration--spring-integration)
8. [Testing Strategy](#testing-strategy)
9. [Implementation Roadmap](#implementation-roadmap)

---

## Overview

This document describes the implementation details for 5 recommendation algorithms and 3 aggregation strategies for the TV Looker recommendation engine. The design maintains the agnostic nature of the engine through a DataProvider architecture.

### Goals

- **Algorithm Diversity:** Implement 5 different recommendation approaches
- **Flexible Aggregation:** Support 3 methods to combine strategy results
- **Agnostic Design:** Strategies never directly access repositories
- **Progressive Implementation:** Sequential delivery with early wins
- **Production Ready:** Comprehensive testing and configuration

### Recommendation Strategies to Implement

1. **Popularity Baseline** - Uses TMDB popularity scores
2. **Content-Based** - Uses item metadata (genre, actors, directors, tags)
3. **Item-Based Collaborative Filtering** - Cosine similarity on item vectors
4. **User-Based Collaborative Filtering** - Cosine similarity on user vectors
5. **Matrix Factorization (SVD)** - Latent factor decomposition

### Aggregation Strategies to Implement

1. **Constant Convex Combination** - Fixed weights for each strategy
2. **Variable Convex Combination** - User profile-based adaptive weights
3. **Ranking-Based (Borda Count)** - Consensus ranking aggregation

### Available Data

- **Interactions:** User ratings (1-5 stars) + watch history
- **Item Metadata:** Genre, year, actors, directors, tags, TMDB popularity
- **User Metadata:** Profile information, interaction count

---

## Implementation Strategy

### Sequential Implementation Approach

```
Phase 1: Foundation (MVP)
├─ 1. Popularity Baseline Strategy
├─ 2. Content-Based Strategy  
└─ 3. Constant Convex Combination Aggregation
   → Deliverable: Working recommendation system

Phase 2: Collaborative Filtering
├─ 4. Item-Based Collaborative Filtering
├─ 5. User-Based Collaborative Filtering
└─ 6. Variable Convex Combination Aggregation
   → Deliverable: Personalized recommendations

Phase 3: Advanced Algorithms
├─ 7. Matrix Factorization (SVD)
└─ 8. Ranking-Based Aggregation (Borda Count)
   → Deliverable: Complete system
```

### Package Structure

```
org.tvl.tvlooker.domain
├── strategy
│   ├── recommendation_strategy/
│   │   ├── RecommendationStrategy.java (interface)
│   │   ├── PopularityStrategy.java
│   │   ├── ContentBasedStrategy.java
│   │   ├── ItemBasedCollaborativeStrategy.java
│   │   ├── UserBasedCollaborativeStrategy.java
│   │   └── MatrixFactorizationStrategy.java
│   │
│   └── aggregation_strategy/
│       ├── AggregationStrategy.java (interface)
│       ├── ConstantConvexCombinationAggregation.java
│       ├── VariableConvexCombinationAggregation.java
│       └── RankingBasedAggregation.java
│
├── motor
│   └── utils/
│       ├── DataProvider.java (interface)
│       ├── RecommendationContext.java (enhanced)
│       ├── provider/
│       │   ├── ItemPopularityProvider.java
│       │   ├── ItemFeatureVectorProvider.java
│       │   ├── UserProfileProvider.java
│       │   ├── ItemSimilarityMatrixProvider.java
│       │   ├── UserSimilarityProvider.java
│       │   └── MatrixFactorizationProvider.java
│       │
│       └── SimilarityComputer.java
│
└── data_structure/
    ├── ScoredItem.java (existing)
    ├── ItemFeatureVector.java
    ├── SimilarityPair.java
    └── SVDFactors.java
```

---

## Data Provider Architecture

### Core Concept

The engine maintains its agnostic nature through the DataProvider pattern:

```
Strategy → RecommendationContext → DataProvider → Data Access → Repositories

- Strategies NEVER access repositories directly
- All data access goes through RecommendationContext
- DataProviders compute and cache data structures
- Providers can request data from other providers
```

### DataProvider Interface

```java
package org.tvl.tvlooker.domain.motor.utils;

/**
 * Base interface for providing computed data structures to strategies.
 * Each DataProvider is responsible for ONE type of data computation.
 */
public interface DataProvider<T> {
    
    /**
     * Returns unique identifier for this provider
     * Used by strategies to request specific data
     */
    String getProviderId();
    
    /**
     * Computes and returns the data structure
     * @param context Access to raw data and other providers
     * @return Computed data structure
     */
    T provide(RecommendationContext context);
    
    /**
     * Whether this provider's result should be cached
     * Default: true
     */
    default boolean isCacheable() {
        return true;
    }
    
    /**
     * Cache expiration time in seconds (if cacheable)
     * Default: 1 hour
     */
    default long getCacheExpirationSeconds() {
        return 3600;
    }
}
```

### RecommendationContext (Enhanced)

```java
package org.tvl.tvlooker.domain.motor.utils;

import java.util.*;

/**
 * Context for recommendation generation.
 * Provides access to raw data and computed data structures through providers.
 */
public class RecommendationContext {
    
    // Raw data (existing)
    private List<User> users;
    private List<Item> items;
    private List<Interaction> interactions;
    
    // DataProvider registry
    private Map<String, DataProvider<?>> dataProviders = new HashMap<>();
    
    // Cache for computed data
    private Map<String, CachedData> dataCache = new HashMap<>();
    
    /**
     * Register a data provider
     */
    public void registerProvider(DataProvider<?> provider) {
        dataProviders.put(provider.getProviderId(), provider);
    }
    
    /**
     * Get computed data from a provider.
     * Strategies call this to get the data they need.
     * 
     * @param providerId Unique identifier of the provider
     * @param dataType Expected return type
     * @return Computed data structure
     * @throws ProviderNotFoundException if provider not registered
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String providerId, Class<T> dataType) {
        // Check cache first
        CachedData cached = dataCache.get(providerId);
        if (cached != null && !cached.isExpired()) {
            return (T) cached.getData();
        }
        
        // Get provider
        DataProvider<?> provider = dataProviders.get(providerId);
        if (provider == null) {
            throw new ProviderNotFoundException(
                "No provider registered with ID: " + providerId);
        }
        
        // Compute data
        T data = (T) provider.provide(this);
        
        // Cache if applicable
        if (provider.isCacheable()) {
            long expirationTime = System.currentTimeMillis() + 
                (provider.getCacheExpirationSeconds() * 1000);
            dataCache.put(providerId, new CachedData(data, expirationTime));
        }
        
        return data;
    }
    
    /**
     * Clear all cached data
     */
    public void clearCache() {
        dataCache.clear();
    }
    
    // Raw data accessors (for providers to use)
    public List<User> getUsers() { return users; }
    public List<Item> getItems() { return items; }
    public List<Interaction> getInteractions() { return interactions; }
    
    // Inner class for cached data
    @AllArgsConstructor
    @Getter
    private static class CachedData {
        private Object data;
        private long expirationTime;
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
}
```

### Provider Registration in Engine

```java
@Component
public class HybridRecommendationEngine implements RecommendationEngine {
    
    private final List<RecommendationStrategy> strategies;
    private final AggregationStrategy aggregationStrategy;
    private final List<DataProvider<?>> dataProviders;
    
    @Autowired
    public HybridRecommendationEngine(
            List<RecommendationStrategy> strategies,
            AggregationStrategy aggregationStrategy,
            List<DataProvider<?>> dataProviders) {
        this.strategies = strategies;
        this.aggregationStrategy = aggregationStrategy;
        this.dataProviders = dataProviders;
    }
    
    @Override
    public List<ScoredItem> recommend(User user, RecommendationContext context) {
        // Register all providers with context
        dataProviders.forEach(context::registerProvider);
        
        // Continue with normal recommendation flow
        validateInputs(user, context);
        List<Item> candidateItems = filterCandidates(user, context);
        Map<String, List<ScoredItem>> strategyResults = 
            executeStrategies(user, candidateItems, context);
        List<ScoredItem> aggregated = 
            aggregationStrategy.aggregate(strategyResults, context);
        return postProcess(aggregated);
    }
}
```

---

## Recommendation Strategies

### 1. Popularity Baseline Strategy

**Purpose:** Simple baseline using TMDB popularity scores. Good for cold start users.

**Algorithm:**
```
For each candidate item:
  score = normalize(item.tmdbPopularity, 0-1)
  explanation = "Trending on TMDB"
```

**Required DataProvider:** `ItemPopularityProvider`

**Implementation:**

```java
@Component
public class PopularityStrategy implements RecommendationStrategy {
    
    @Override
    public String getStrategyName() {
        return "popularity";
    }
    
    @Override
    public List<ScoredItem> recommend(
            User user, 
            List<Item> candidateItems, 
            RecommendationContext context) {
        
        // Get popularity data from provider
        Map<Long, Double> popularityScores = 
            context.getData("item-popularity", Map.class);
        
        return candidateItems.stream()
            .map(item -> ScoredItem.builder()
                .item(item)
                .score(popularityScores.getOrDefault(item.getId(), 0.0))
                .explanation("Trending on TMDB")
                .sourceStrategy(getStrategyName())
                .build())
            .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
            .collect(Collectors.toList());
    }
}
```

**ItemPopularityProvider:**

```java
@Component
public class ItemPopularityProvider implements DataProvider<Map<Long, Double>> {
    
    @Override
    public String getProviderId() {
        return "item-popularity";
    }
    
    @Override
    public Map<Long, Double> provide(RecommendationContext context) {
        Map<Long, Double> popularityScores = new HashMap<>();
        
        // Find max popularity for normalization
        double maxPopularity = context.getItems().stream()
            .mapToDouble(Item::getTmdbPopularity)
            .max()
            .orElse(1.0);
        
        // Normalize all scores to [0, 1]
        for (Item item : context.getItems()) {
            double normalized = item.getTmdbPopularity() / maxPopularity;
            popularityScores.put(item.getId(), normalized);
        }
        
        return popularityScores;
    }
    
    @Override
    public long getCacheExpirationSeconds() {
        return 86400; // 24 hours - popularity changes slowly
    }
}
```

---

### 2. Content-Based Strategy

**Purpose:** Recommend items similar to user's watched items based on content features.

**Algorithm:**
```
1. Build user profile from watch history:
   - Extract genres, actors, directors, tags from watched items
   - Weight by rating (higher rated = more important)
   
2. For each candidate item:
   - Compute content similarity to user profile
   - Use cosine similarity on TF-IDF weighted feature vectors
   
3. Score = cosine similarity
   Explanation = "Similar to items you enjoyed"
```

**Required DataProviders:** 
- `ItemFeatureVectorProvider` - TF-IDF weighted item vectors
- `UserProfileProvider` - Aggregated user preference vectors

**Feature Vector Data Structure:**

```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItemFeatureVector {
    private Map<String, Double> genreWeights;
    private Map<String, Double> actorWeights;
    private Map<String, Double> directorWeights;
    private Map<String, Double> tagWeights;
    
    /**
     * Compute cosine similarity with another vector
     */
    public double cosineSimilarity(ItemFeatureVector other) {
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        // Compute for genres
        dotProduct += computeDotProduct(this.genreWeights, other.genreWeights);
        norm1 += computeNorm(this.genreWeights);
        norm2 += computeNorm(other.genreWeights);
        
        // Compute for actors
        dotProduct += computeDotProduct(this.actorWeights, other.actorWeights);
        norm1 += computeNorm(this.actorWeights);
        norm2 += computeNorm(other.actorWeights);
        
        // Compute for directors
        dotProduct += computeDotProduct(this.directorWeights, other.directorWeights);
        norm1 += computeNorm(this.directorWeights);
        norm2 += computeNorm(other.directorWeights);
        
        // Compute for tags
        dotProduct += computeDotProduct(this.tagWeights, other.tagWeights);
        norm1 += computeNorm(this.tagWeights);
        norm2 += computeNorm(other.tagWeights);
        
        if (norm1 == 0 || norm2 == 0) return 0.0;
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    private double computeDotProduct(Map<String, Double> v1, Map<String, Double> v2) {
        return v1.entrySet().stream()
            .mapToDouble(e -> e.getValue() * v2.getOrDefault(e.getKey(), 0.0))
            .sum();
    }
    
    private double computeNorm(Map<String, Double> v) {
        return v.values().stream()
            .mapToDouble(x -> x * x)
            .sum();
    }
}
```

**Implementation:**

```java
@Component
public class ContentBasedStrategy implements RecommendationStrategy {
    
    @Override
    public String getStrategyName() {
        return "content-based";
    }
    
    @Override
    public List<ScoredItem> recommend(
            User user, 
            List<Item> candidateItems, 
            RecommendationContext context) {
        
        // Get pre-computed data from providers
        Map<Long, ItemFeatureVector> itemVectors = 
            context.getData("item-feature-vectors", Map.class);
        Map<Long, ItemFeatureVector> userProfiles = 
            context.getData("user-content-profiles", Map.class);
        
        ItemFeatureVector userProfile = userProfiles.get(user.getId());
        if (userProfile == null) {
            // Cold start: no profile yet
            return Collections.emptyList();
        }
        
        return candidateItems.stream()
            .map(item -> {
                ItemFeatureVector itemVector = itemVectors.get(item.getId());
                double similarity = userProfile.cosineSimilarity(itemVector);
                
                return ScoredItem.builder()
                    .item(item)
                    .score(similarity)
                    .explanation("Similar to items you enjoyed")
                    .sourceStrategy(getStrategyName())
                    .build();
            })
            .filter(scored -> scored.getScore() > 0)
            .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
            .collect(Collectors.toList());
    }
}
```

**ItemFeatureVectorProvider:**

```java
@Component
public class ItemFeatureVectorProvider 
        implements DataProvider<Map<Long, ItemFeatureVector>> {
    
    @Value("${recommendation.content.tfidf.enabled:true}")
    private boolean useTfIdf;
    
    @Override
    public String getProviderId() {
        return "item-feature-vectors";
    }
    
    @Override
    public Map<Long, ItemFeatureVector> provide(RecommendationContext context) {
        List<Item> items = context.getItems();
        Map<Long, ItemFeatureVector> vectors = new HashMap<>();
        
        // Compute IDF for each feature type
        Map<String, Double> genreIDF = computeIDF(items, Item::getGenres);
        Map<String, Double> actorIDF = computeIDF(items, Item::getActors);
        Map<String, Double> directorIDF = computeIDF(items, Item::getDirectors);
        Map<String, Double> tagIDF = computeIDF(items, Item::getTags);
        
        // Build feature vector for each item
        for (Item item : items) {
            ItemFeatureVector vector = ItemFeatureVector.builder()
                .genreWeights(computeTFIDF(item.getGenres(), genreIDF))
                .actorWeights(computeTFIDF(item.getActors(), actorIDF))
                .directorWeights(computeTFIDF(item.getDirectors(), directorIDF))
                .tagWeights(computeTFIDF(item.getTags(), tagIDF))
                .build();
            
            vectors.put(item.getId(), vector);
        }
        
        return vectors;
    }
    
    /**
     * Compute IDF (Inverse Document Frequency) for features
     * IDF(feature) = log(N / df) where df = document frequency
     */
    private Map<String, Double> computeIDF(
            List<Item> items,
            Function<Item, List<String>> featureExtractor) {
        
        int N = items.size();
        Map<String, Integer> documentFrequency = new HashMap<>();
        
        // Count how many items have each feature
        for (Item item : items) {
            List<String> features = featureExtractor.apply(item);
            if (features != null) {
                features.stream()
                    .distinct()
                    .forEach(f -> documentFrequency.merge(f, 1, Integer::sum));
            }
        }
        
        // Compute IDF
        Map<String, Double> idf = new HashMap<>();
        documentFrequency.forEach((feature, df) -> {
            idf.put(feature, Math.log((double) N / df));
        });
        
        return idf;
    }
    
    /**
     * Compute TF-IDF weights for a list of features
     */
    private Map<String, Double> computeTFIDF(
            List<String> features,
            Map<String, Double> idf) {
        
        if (features == null || features.isEmpty()) {
            return new HashMap<>();
        }
        
        // Compute term frequency
        Map<String, Long> tf = features.stream()
            .collect(Collectors.groupingBy(f -> f, Collectors.counting()));
        
        // Compute TF-IDF
        Map<String, Double> tfidf = new HashMap<>();
        tf.forEach((feature, count) -> {
            double tfValue = useTfIdf ? count.doubleValue() : 1.0;
            double idfValue = idf.getOrDefault(feature, 1.0);
            tfidf.put(feature, tfValue * idfValue);
        });
        
        return tfidf;
    }
    
    @Override
    public long getCacheExpirationSeconds() {
        return 86400; // 24 hours - item metadata changes rarely
    }
}
```

**UserProfileProvider:**

```java
@Component
public class UserProfileProvider 
        implements DataProvider<Map<Long, ItemFeatureVector>> {
    
    @Override
    public String getProviderId() {
        return "user-content-profiles";
    }
    
    @Override
    public Map<Long, ItemFeatureVector> provide(RecommendationContext context) {
        Map<Long, ItemFeatureVector> userProfiles = new HashMap<>();
        
        // Get item feature vectors
        Map<Long, ItemFeatureVector> itemVectors = 
            context.getData("item-feature-vectors", Map.class);
        
        // Build profile for each user
        for (User user : context.getUsers()) {
            // Get user's interactions (ratings + watches)
            List<Interaction> userInteractions = context.getInteractions()
                .stream()
                .filter(i -> i.getUserId().equals(user.getId()))
                .collect(Collectors.toList());
            
            if (userInteractions.isEmpty()) {
                continue; // Skip users with no history
            }
            
            // Aggregate item vectors weighted by rating
            ItemFeatureVector profile = aggregateVectors(
                userInteractions, itemVectors);
            
            userProfiles.put(user.getId(), profile);
        }
        
        return userProfiles;
    }
    
    /**
     * Aggregate item vectors weighted by user ratings
     */
    private ItemFeatureVector aggregateVectors(
            List<Interaction> interactions,
            Map<Long, ItemFeatureVector> itemVectors) {
        
        Map<String, Double> genreWeights = new HashMap<>();
        Map<String, Double> actorWeights = new HashMap<>();
        Map<String, Double> directorWeights = new HashMap<>();
        Map<String, Double> tagWeights = new HashMap<>();
        
        double totalWeight = 0.0;
        
        for (Interaction interaction : interactions) {
            ItemFeatureVector itemVector = itemVectors.get(interaction.getItemId());
            if (itemVector == null) continue;
            
            // Use rating as weight (default to 1.0 for watches without rating)
            double weight = interaction.getRating() != null ? 
                interaction.getRating() : 1.0;
            totalWeight += weight;
            
            // Aggregate each feature type
            aggregateFeatureMap(genreWeights, itemVector.getGenreWeights(), weight);
            aggregateFeatureMap(actorWeights, itemVector.getActorWeights(), weight);
            aggregateFeatureMap(directorWeights, itemVector.getDirectorWeights(), weight);
            aggregateFeatureMap(tagWeights, itemVector.getTagWeights(), weight);
        }
        
        // Normalize by total weight
        if (totalWeight > 0) {
            normalizeMap(genreWeights, totalWeight);
            normalizeMap(actorWeights, totalWeight);
            normalizeMap(directorWeights, totalWeight);
            normalizeMap(tagWeights, totalWeight);
        }
        
        return ItemFeatureVector.builder()
            .genreWeights(genreWeights)
            .actorWeights(actorWeights)
            .directorWeights(directorWeights)
            .tagWeights(tagWeights)
            .build();
    }
    
    private void aggregateFeatureMap(
            Map<String, Double> target,
            Map<String, Double> source,
            double weight) {
        source.forEach((feature, value) -> 
            target.merge(feature, value * weight, Double::sum));
    }
    
    private void normalizeMap(Map<String, Double> map, double divisor) {
        map.replaceAll((k, v) -> v / divisor);
    }
    
    @Override
    public long getCacheExpirationSeconds() {
        return 3600; // 1 hour - user preferences change more frequently
    }
}
```

---

### 3. Item-Based Collaborative Filtering

**Purpose:** Recommend items similar to items the user has rated/watched.

**Algorithm:**
```
1. Pre-compute item-item similarity matrix:
   - For each pair of items
   - Find users who interacted with both
   - Compute cosine similarity of their rating vectors
   
2. For recommendation:
   - Get items user has rated/watched
   - Find similar items using similarity matrix
   - Aggregate scores: weighted sum of (similarity * user's rating)
   
3. Score = normalized weighted sum
   Explanation = "Similar to [item] you watched"
```

**Required DataProvider:** `ItemSimilarityMatrixProvider`

**Data Structures:**

```java
@Data
@AllArgsConstructor
public class SimilarityPair {
    private Long itemId;
    private Double similarity;
}
```

**Implementation:**

```java
@Component
public class ItemBasedCollaborativeStrategy implements RecommendationStrategy {
    
    @Override
    public String getStrategyName() {
        return "item-collaborative";
    }
    
    @Override
    public List<ScoredItem> recommend(
            User user,
            List<Item> candidateItems,
            RecommendationContext context) {
        
        // Get similarity matrix
        Map<Long, List<SimilarityPair>> similarityMatrix = 
            context.getData("item-similarity-matrix", Map.class);
        
        // Get user's rated/watched items
        List<Interaction> userInteractions = context.getInteractions()
            .stream()
            .filter(i -> i.getUserId().equals(user.getId()))
            .collect(Collectors.toList());
        
        if (userInteractions.isEmpty()) {
            return Collections.emptyList(); // Cold start
        }
        
        // Score each candidate item
        Map<Long, Double> itemScores = new HashMap<>();
        Map<Long, Double> itemWeights = new HashMap<>();
        
        for (Interaction interaction : userInteractions) {
            Long ratedItemId = interaction.getItemId();
            Double userRating = interaction.getRating() != null ? 
                interaction.getRating() : 1.0;
            
            List<SimilarityPair> similarItems = 
                similarityMatrix.getOrDefault(ratedItemId, Collections.emptyList());
            
            for (SimilarityPair pair : similarItems) {
                // Only consider candidate items
                if (candidateItems.stream()
                        .anyMatch(item -> item.getId().equals(pair.getItemId()))) {
                    
                    double score = pair.getSimilarity() * userRating;
                    itemScores.merge(pair.getItemId(), score, Double::sum);
                    itemWeights.merge(pair.getItemId(), pair.getSimilarity(), Double::sum);
                }
            }
        }
        
        // Normalize scores and create ScoredItems
        return candidateItems.stream()
            .filter(item -> itemScores.containsKey(item.getId()))
            .map(item -> {
                double rawScore = itemScores.get(item.getId());
                double weight = itemWeights.get(item.getId());
                double normalizedScore = weight > 0 ? rawScore / weight : 0.0;
                
                // Normalize to [0, 1]
                normalizedScore = Math.min(1.0, normalizedScore / 5.0);
                
                return ScoredItem.builder()
                    .item(item)
                    .score(normalizedScore)
                    .explanation("Similar to items you watched")
                    .sourceStrategy(getStrategyName())
                    .build();
            })
            .filter(scored -> scored.getScore() > 0)
            .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
            .collect(Collectors.toList());
    }
}
```

**ItemSimilarityMatrixProvider:**

```java
@Component
public class ItemSimilarityMatrixProvider 
        implements DataProvider<Map<Long, List<SimilarityPair>>> {
    
    @Value("${recommendation.item-cf.top-k:50}")
    private int topK;
    
    @Autowired
    private SimilarityComputer similarityComputer;
    
    @Override
    public String getProviderId() {
        return "item-similarity-matrix";
    }
    
    @Override
    public Map<Long, List<SimilarityPair>> provide(RecommendationContext context) {
        
        // Build item-user rating matrix
        // Map: itemId -> (userId -> rating)
        Map<Long, Map<Long, Double>> itemUserRatings = new HashMap<>();
        
        for (Interaction interaction : context.getInteractions()) {
            Long itemId = interaction.getItemId();
            Long userId = interaction.getUserId();
            Double rating = interaction.getRating() != null ? 
                interaction.getRating() : 1.0;
            
            itemUserRatings
                .computeIfAbsent(itemId, k -> new HashMap<>())
                .put(userId, rating);
        }
        
        // Compute similarity for each item pair
        List<Item> items = context.getItems();
        Map<Long, List<SimilarityPair>> similarityMatrix = new HashMap<>();
        
        for (int i = 0; i < items.size(); i++) {
            Long itemId1 = items.get(i).getId();
            Map<Long, Double> ratings1 = itemUserRatings.get(itemId1);
            
            if (ratings1 == null || ratings1.isEmpty()) {
                continue; // Skip items with no ratings
            }
            
            List<SimilarityPair> similarities = new ArrayList<>();
            
            for (int j = 0; j < items.size(); j++) {
                if (i == j) continue;
                
                Long itemId2 = items.get(j).getId();
                Map<Long, Double> ratings2 = itemUserRatings.get(itemId2);
                
                if (ratings2 == null || ratings2.isEmpty()) {
                    continue;
                }
                
                // Compute cosine similarity
                double similarity = similarityComputer.cosineSimilarity(
                    ratings1, ratings2);
                
                if (similarity > 0) {
                    similarities.add(new SimilarityPair(itemId2, similarity));
                }
            }
            
            // Keep only top K similar items
            similarities.sort(Comparator.comparing(
                SimilarityPair::getSimilarity).reversed());
            
            List<SimilarityPair> topKSimilar = similarities.stream()
                .limit(topK)
                .collect(Collectors.toList());
            
            similarityMatrix.put(itemId1, topKSimilar);
        }
        
        return similarityMatrix;
    }
    
    @Override
    public long getCacheExpirationSeconds() {
        return 604800; // 1 week - recompute weekly
    }
}
```

---

### 4. User-Based Collaborative Filtering

**Purpose:** Recommend items that similar users enjoyed.

**Algorithm:**
```
1. Find similar users to target user:
   - Compute cosine similarity on user rating vectors
   - Select top K similar users (K=30)
   
2. For each candidate item:
   - Find similar users who rated/watched it
   - Score = weighted average of their ratings
   - Weight = user similarity
   
3. Explanation = "Users similar to you enjoyed this"
```

**Required DataProvider:** `UserSimilarityProvider`

**Implementation:**

```java
@Component
public class UserBasedCollaborativeStrategy implements RecommendationStrategy {
    
    @Value("${recommendation.user-cf.min-interactions:5}")
    private int minInteractions;
    
    @Override
    public String getStrategyName() {
        return "user-collaborative";
    }
    
    @Override
    public List<ScoredItem> recommend(
            User user,
            List<Item> candidateItems,
            RecommendationContext context) {
        
        // Check if user has enough interactions
        long userInteractionCount = context.getInteractions()
            .stream()
            .filter(i -> i.getUserId().equals(user.getId()))
            .count();
        
        if (userInteractionCount < minInteractions) {
            return Collections.emptyList(); // Cold start
        }
        
        // Get user similarity data
        Map<Long, List<SimilarityPair>> userSimilarities = 
            context.getData("user-similarity-matrix", Map.class);
        
        List<SimilarityPair> similarUsers = 
            userSimilarities.getOrDefault(user.getId(), Collections.emptyList());
        
        if (similarUsers.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Get interactions of similar users
        Set<Long> similarUserIds = similarUsers.stream()
            .map(SimilarityPair::getItemId) // In this context, itemId is actually userId
            .collect(Collectors.toSet());
        
        Map<Long, List<Interaction>> itemInteractions = context.getInteractions()
            .stream()
            .filter(i -> similarUserIds.contains(i.getUserId()))
            .collect(Collectors.groupingBy(Interaction::getItemId));
        
        // Score each candidate item
        return candidateItems.stream()
            .map(item -> {
                List<Interaction> interactions = 
                    itemInteractions.getOrDefault(item.getId(), Collections.emptyList());
                
                if (interactions.isEmpty()) {
                    return null;
                }
                
                // Weighted average of ratings
                double scoreSum = 0.0;
                double weightSum = 0.0;
                
                for (Interaction interaction : interactions) {
                    // Find similarity to this user
                    Optional<SimilarityPair> similarityOpt = similarUsers.stream()
                        .filter(p -> p.getItemId().equals(interaction.getUserId()))
                        .findFirst();
                    
                    if (similarityOpt.isPresent()) {
                        double similarity = similarityOpt.get().getSimilarity();
                        double rating = interaction.getRating() != null ? 
                            interaction.getRating() : 1.0;
                        
                        scoreSum += similarity * rating;
                        weightSum += similarity;
                    }
                }
                
                if (weightSum == 0) return null;
                
                double score = scoreSum / weightSum;
                // Normalize to [0, 1]
                score = Math.min(1.0, score / 5.0);
                
                return ScoredItem.builder()
                    .item(item)
                    .score(score)
                    .explanation("Users similar to you enjoyed this")
                    .sourceStrategy(getStrategyName())
                    .build();
            })
            .filter(Objects::nonNull)
            .filter(scored -> scored.getScore() > 0)
            .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
            .collect(Collectors.toList());
    }
}
```

**UserSimilarityProvider:**

```java
@Component
public class UserSimilarityProvider 
        implements DataProvider<Map<Long, List<SimilarityPair>>> {
    
    @Value("${recommendation.user-cf.top-k:30}")
    private int topK;
    
    @Autowired
    private SimilarityComputer similarityComputer;
    
    @Override
    public String getProviderId() {
        return "user-similarity-matrix";
    }
    
    @Override
    public Map<Long, List<SimilarityPair>> provide(RecommendationContext context) {
        
        // Build user-item rating matrix
        // Map: userId -> (itemId -> rating)
        Map<Long, Map<Long, Double>> userItemRatings = new HashMap<>();
        
        for (Interaction interaction : context.getInteractions()) {
            Long userId = interaction.getUserId();
            Long itemId = interaction.getItemId();
            Double rating = interaction.getRating() != null ? 
                interaction.getRating() : 1.0;
            
            userItemRatings
                .computeIfAbsent(userId, k -> new HashMap<>())
                .put(itemId, rating);
        }
        
        // Compute similarity for each user pair
        List<User> users = context.getUsers();
        Map<Long, List<SimilarityPair>> similarityMatrix = new HashMap<>();
        
        for (int i = 0; i < users.size(); i++) {
            Long userId1 = users.get(i).getId();
            Map<Long, Double> ratings1 = userItemRatings.get(userId1);
            
            if (ratings1 == null || ratings1.size() < 3) {
                continue; // Skip users with too few ratings
            }
            
            List<SimilarityPair> similarities = new ArrayList<>();
            
            for (int j = 0; j < users.size(); j++) {
                if (i == j) continue;
                
                Long userId2 = users.get(j).getId();
                Map<Long, Double> ratings2 = userItemRatings.get(userId2);
                
                if (ratings2 == null || ratings2.size() < 3) {
                    continue;
                }
                
                // Compute cosine similarity
                double similarity = similarityComputer.cosineSimilarity(
                    ratings1, ratings2);
                
                if (similarity > 0) {
                    // Store userId in itemId field (reusing SimilarityPair)
                    similarities.add(new SimilarityPair(userId2, similarity));
                }
            }
            
            // Keep only top K similar users
            similarities.sort(Comparator.comparing(
                SimilarityPair::getSimilarity).reversed());
            
            List<SimilarityPair> topKSimilar = similarities.stream()
                .limit(topK)
                .collect(Collectors.toList());
            
            similarityMatrix.put(userId1, topKSimilar);
        }
        
        return similarityMatrix;
    }
    
    @Override
    public long getCacheExpirationSeconds() {
        return 3600; // 1 hour - user preferences change
    }
}
```

---

### 5. Matrix Factorization (SVD)

**Purpose:** Discover latent factors through matrix decomposition.

**Algorithm:**
```
1. Build user-item interaction matrix R (sparse)
   - Rows = users, Columns = items
   - Values = ratings (or 1.0 for watched)
   
2. Decompose: R ≈ U × Σ × V^T
   - U = user factors (users × k)
   - V = item factors (items × k)
   - k = latent dimensions (e.g., 50)
   
3. For recommendation:
   - Get user factor vector u
   - Get item factor vector v
   - Score = u · v (dot product)
   
4. Explanation = "Based on your overall preferences"
```

**Required DataProvider:** `MatrixFactorizationProvider`

**Data Structures:**

```java
@Data
@AllArgsConstructor
public class SVDFactors {
    private RealMatrix userFactors;    // U matrix
    private RealMatrix itemFactors;    // V matrix
    private RealMatrix singularValues; // Σ matrix
    private Map<Long, Integer> userIdToIndex;
    private Map<Long, Integer> itemIdToIndex;
}
```

**Implementation:**

```java
@Component
public class MatrixFactorizationStrategy implements RecommendationStrategy {
    
    @Override
    public String getStrategyName() {
        return "matrix-factorization";
    }
    
    @Override
    public List<ScoredItem> recommend(
            User user,
            List<Item> candidateItems,
            RecommendationContext context) {
        
        // Get SVD factors
        SVDFactors svdFactors = 
            context.getData("svd-factors", SVDFactors.class);
        
        // Get user index
        Integer userIndex = svdFactors.getUserIdToIndex().get(user.getId());
        if (userIndex == null) {
            return Collections.emptyList(); // User not in training set
        }
        
        // Get user factor vector
        RealVector userVector = svdFactors.getUserFactors().getRowVector(userIndex);
        
        // Score each candidate item
        return candidateItems.stream()
            .map(item -> {
                Integer itemIndex = svdFactors.getItemIdToIndex().get(item.getId());
                if (itemIndex == null) {
                    return null; // Item not in training set
                }
                
                // Get item factor vector
                RealVector itemVector = svdFactors.getItemFactors().getRowVector(itemIndex);
                
                // Compute dot product
                double score = userVector.dotProduct(itemVector);
                
                // Normalize to [0, 1] (assuming scores are in reasonable range)
                score = Math.max(0, Math.min(1, score / 5.0));
                
                return ScoredItem.builder()
                    .item(item)
                    .score(score)
                    .explanation("Based on your overall preferences")
                    .sourceStrategy(getStrategyName())
                    .build();
            })
            .filter(Objects::nonNull)
            .filter(scored -> scored.getScore() > 0)
            .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
            .collect(Collectors.toList());
    }
}
```

**MatrixFactorizationProvider:**

```java
@Component
public class MatrixFactorizationProvider implements DataProvider<SVDFactors> {
    
    @Value("${recommendation.mf.latent-factors:50}")
    private int latentFactors;
    
    @Override
    public String getProviderId() {
        return "svd-factors";
    }
    
    @Override
    public SVDFactors provide(RecommendationContext context) {
        List<User> users = context.getUsers();
        List<Item> items = context.getItems();
        List<Interaction> interactions = context.getInteractions();
        
        // Create index mappings
        Map<Long, Integer> userIdToIndex = new HashMap<>();
        Map<Long, Integer> itemIdToIndex = new HashMap<>();
        
        for (int i = 0; i < users.size(); i++) {
            userIdToIndex.put(users.get(i).getId(), i);
        }
        for (int i = 0; i < items.size(); i++) {
            itemIdToIndex.put(items.get(i).getId(), i);
        }
        
        // Build rating matrix
        RealMatrix ratingMatrix = MatrixUtils.createRealMatrix(
            users.size(), items.size());
        
        for (Interaction interaction : interactions) {
            Integer userIdx = userIdToIndex.get(interaction.getUserId());
            Integer itemIdx = itemIdToIndex.get(interaction.getItemId());
            
            if (userIdx != null && itemIdx != null) {
                double rating = interaction.getRating() != null ? 
                    interaction.getRating() : 1.0;
                ratingMatrix.setEntry(userIdx, itemIdx, rating);
            }
        }
        
        // Perform SVD
        SingularValueDecomposition svd = 
            new SingularValueDecomposition(ratingMatrix);
        
        RealMatrix U = svd.getU(); // User factors
        RealMatrix S = svd.getS(); // Singular values
        RealMatrix V = svd.getV(); // Item factors
        
        // Reduce to k dimensions
        int k = Math.min(latentFactors, Math.min(U.getColumnDimension(), 
                                                   V.getColumnDimension()));
        
        RealMatrix Uk = U.getSubMatrix(0, U.getRowDimension() - 1, 0, k - 1);
        RealMatrix Sk = S.getSubMatrix(0, k - 1, 0, k - 1);
        RealMatrix Vk = V.getSubMatrix(0, V.getRowDimension() - 1, 0, k - 1);
        
        // Multiply U by S to get final user factors
        RealMatrix userFactors = Uk.multiply(Sk);
        
        return new SVDFactors(userFactors, Vk, Sk, userIdToIndex, itemIdToIndex);
    }
    
    @Override
    public long getCacheExpirationSeconds() {
        return 604800; // 1 week - retrain weekly
    }
}
```

---

## Aggregation Strategies

### 1. Constant Convex Combination

**Purpose:** Combine strategy results using fixed weights.

**Algorithm:**
```
For each item that appears in any strategy results:
  finalScore = Σ (weight_i × strategy_i_score)
  where: Σ weight_i = 1.0, all weights >= 0
  
If item missing from a strategy: treat score as 0.0
Normalize remaining weights to sum to 1.0
```

**Configuration:**

```java
@Component
public class ConstantConvexCombinationAggregation implements AggregationStrategy {
    
    @Value("${recommendation.weights.popularity:0.15}")
    private double popularityWeight;
    
    @Value("${recommendation.weights.content:0.25}")
    private double contentWeight;
    
    @Value("${recommendation.weights.item-collaborative:0.25}")
    private double itemCollaborativeWeight;
    
    @Value("${recommendation.weights.user-collaborative:0.25}")
    private double userCollaborativeWeight;
    
    @Value("${recommendation.weights.matrix-factorization:0.10}")
    private double matrixFactorizationWeight;
    
    private Map<String, Double> weights;
    
    @PostConstruct
    public void init() {
        weights = new HashMap<>();
        weights.put("popularity", popularityWeight);
        weights.put("content-based", contentWeight);
        weights.put("item-collaborative", itemCollaborativeWeight);
        weights.put("user-collaborative", userCollaborativeWeight);
        weights.put("matrix-factorization", matrixFactorizationWeight);
        
        // Validate weights sum to 1.0
        double sum = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - 1.0) > 0.01) {
            throw new InvalidConfigurationException(
                "Strategy weights must sum to 1.0, got: " + sum);
        }
    }
    
    @Override
    public String getAggregationName() {
        return "constant-convex-combination";
    }
    
    @Override
    public List<ScoredItem> aggregate(
            Map<String, List<ScoredItem>> strategyResults,
            RecommendationContext context) {
        
        // Collect all unique items
        Map<Long, Item> allItems = new HashMap<>();
        strategyResults.values().forEach(results -> 
            results.forEach(scored -> 
                allItems.put(scored.getItem().getId(), scored.getItem())));
        
        // Compute weighted score for each item
        Map<Long, Double> itemScores = new HashMap<>();
        Map<Long, List<String>> itemExplanations = new HashMap<>();
        
        for (Long itemId : allItems.keySet()) {
            double finalScore = 0.0;
            List<String> explanations = new ArrayList<>();
            double activeWeightSum = 0.0;
            
            for (Map.Entry<String, List<ScoredItem>> entry : strategyResults.entrySet()) {
                String strategyName = entry.getKey();
                List<ScoredItem> results = entry.getValue();
                
                Optional<ScoredItem> scoredOpt = results.stream()
                    .filter(s -> s.getItem().getId().equals(itemId))
                    .findFirst();
                
                if (scoredOpt.isPresent()) {
                    ScoredItem scored = scoredOpt.get();
                    double weight = weights.getOrDefault(strategyName, 0.0);
                    
                    finalScore += weight * scored.getScore();
                    activeWeightSum += weight;
                    explanations.add(scored.getExplanation());
                }
            }
            
            // Normalize by active weights
            if (activeWeightSum > 0) {
                finalScore = finalScore / activeWeightSum;
            }
            
            itemScores.put(itemId, finalScore);
            itemExplanations.put(itemId, explanations);
        }
        
        // Create final scored items
        return allItems.entrySet().stream()
            .map(entry -> {
                Long itemId = entry.getKey();
                Item item = entry.getValue();
                
                return ScoredItem.builder()
                    .item(item)
                    .score(itemScores.get(itemId))
                    .explanation(String.join(" | ", itemExplanations.get(itemId)))
                    .sourceStrategy(getAggregationName())
                    .build();
            })
            .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
            .collect(Collectors.toList());
    }
}
```

---

### 2. Variable Convex Combination

**Purpose:** Adapt weights based on user profile maturity.

**Algorithm:**
```
1. Analyze user profile:
   - interactionCount = number of ratings/watches
   - profileCompleteness = filled profile fields / total fields
   
2. Determine maturity level:
   - NEW: < 5 interactions
   - MEDIUM: 5-20 interactions
   - EXPERIENCED: > 20 interactions
   
3. Adjust weights based on level:
   - NEW: Favor popularity and content-based
   - MEDIUM: Balance all strategies
   - EXPERIENCED: Favor collaborative and MF
   
4. Apply weighted combination with adjusted weights
```

**User Maturity Levels:**

```java
public enum UserMaturityLevel {
    NEW,
    MEDIUM,
    EXPERIENCED
}
```

**Implementation:**

```java
@Component
public class VariableConvexCombinationAggregation implements AggregationStrategy {
    
    // Weight configurations for each maturity level
    private static final Map<UserMaturityLevel, Map<String, Double>> WEIGHT_CONFIGURATIONS = 
        Map.of(
            UserMaturityLevel.NEW, Map.of(
                "popularity", 0.40,
                "content-based", 0.30,
                "item-collaborative", 0.15,
                "user-collaborative", 0.15,
                "matrix-factorization", 0.0
            ),
            UserMaturityLevel.MEDIUM, Map.of(
                "popularity", 0.20,
                "content-based", 0.25,
                "item-collaborative", 0.25,
                "user-collaborative", 0.25,
                "matrix-factorization", 0.05
            ),
            UserMaturityLevel.EXPERIENCED, Map.of(
                "popularity", 0.10,
                "content-based", 0.20,
                "item-collaborative", 0.30,
                "user-collaborative", 0.30,
                "matrix-factorization", 0.10
            )
        );
    
    @Override
    public String getAggregationName() {
        return "variable-convex-combination";
    }
    
    @Override
    public List<ScoredItem> aggregate(
            Map<String, List<ScoredItem>> strategyResults,
            RecommendationContext context) {
        
        // Determine user maturity (extract from first strategy result)
        User user = extractUser(strategyResults, context);
        UserMaturityLevel maturityLevel = determineMaturityLevel(user, context);
        
        // Get weights for this maturity level
        Map<String, Double> weights = WEIGHT_CONFIGURATIONS.get(maturityLevel);
        
        // Use same logic as ConstantConvexCombination but with adaptive weights
        return aggregateWithWeights(strategyResults, weights);
    }
    
    private User extractUser(
            Map<String, List<ScoredItem>> strategyResults,
            RecommendationContext context) {
        // Extract user from context (implementation depends on how user is passed)
        // For now, we'll need to pass user through context or modify interface
        // This is a design decision to discuss
        return null; // Placeholder
    }
    
    private UserMaturityLevel determineMaturityLevel(User user, RecommendationContext context) {
        long interactionCount = context.getInteractions()
            .stream()
            .filter(i -> i.getUserId().equals(user.getId()))
            .count();
        
        if (interactionCount < 5) {
            return UserMaturityLevel.NEW;
        } else if (interactionCount <= 20) {
            return UserMaturityLevel.MEDIUM;
        } else {
            return UserMaturityLevel.EXPERIENCED;
        }
    }
    
    private List<ScoredItem> aggregateWithWeights(
            Map<String, List<ScoredItem>> strategyResults,
            Map<String, Double> weights) {
        
        // Same logic as ConstantConvexCombination
        Map<Long, Item> allItems = new HashMap<>();
        strategyResults.values().forEach(results -> 
            results.forEach(scored -> 
                allItems.put(scored.getItem().getId(), scored.getItem())));
        
        Map<Long, Double> itemScores = new HashMap<>();
        Map<Long, List<String>> itemExplanations = new HashMap<>();
        
        for (Long itemId : allItems.keySet()) {
            double finalScore = 0.0;
            List<String> explanations = new ArrayList<>();
            double activeWeightSum = 0.0;
            
            for (Map.Entry<String, List<ScoredItem>> entry : strategyResults.entrySet()) {
                String strategyName = entry.getKey();
                List<ScoredItem> results = entry.getValue();
                
                Optional<ScoredItem> scoredOpt = results.stream()
                    .filter(s -> s.getItem().getId().equals(itemId))
                    .findFirst();
                
                if (scoredOpt.isPresent()) {
                    ScoredItem scored = scoredOpt.get();
                    double weight = weights.getOrDefault(strategyName, 0.0);
                    
                    finalScore += weight * scored.getScore();
                    activeWeightSum += weight;
                    explanations.add(scored.getExplanation());
                }
            }
            
            if (activeWeightSum > 0) {
                finalScore = finalScore / activeWeightSum;
            }
            
            itemScores.put(itemId, finalScore);
            itemExplanations.put(itemId, explanations);
        }
        
        return allItems.entrySet().stream()
            .map(entry -> {
                Long itemId = entry.getKey();
                Item item = entry.getValue();
                
                return ScoredItem.builder()
                    .item(item)
                    .score(itemScores.get(itemId))
                    .explanation(String.join(" | ", itemExplanations.get(itemId)))
                    .sourceStrategy(getAggregationName())
                    .build();
            })
            .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
            .collect(Collectors.toList());
    }
}
```

**Note:** The Variable aggregation requires access to the User object. This may require modifying the `AggregationStrategy` interface to include the user parameter, or storing it in the `RecommendationContext`.

---

### 3. Ranking-Based Aggregation (Borda Count)

**Purpose:** Combine strategies based on ranking consensus rather than absolute scores.

**Algorithm:**
```
1. Each strategy produces ranked list of items

2. Borda Count scoring:
   For each item in strategy results:
     points = (N - rank + 1)
     where N = total items in that strategy's results
     
3. Aggregate points across all strategies:
   totalPoints = Σ bordaPoints_i
   
4. Normalize score = totalPoints / maxPossiblePoints

5. Sort by score descending
```

**Example:**
```
Strategy A: [Item1(rank 1), Item2(rank 2), Item3(rank 3)] → N=3
Strategy B: [Item2(rank 1), Item1(rank 3), Item4(rank 2)] → N=3

Borda Points:
  Item1: (3-1+1) + (3-3+1) = 3 + 1 = 4 points
  Item2: (3-2+1) + (3-1+1) = 2 + 3 = 5 points (highest)
  Item3: (3-3+1) + 0 = 1 point
  Item4: 0 + (3-2+1) = 2 points

Normalized scores (max = 6):
  Item2: 5/6 = 0.833
  Item1: 4/6 = 0.667
  Item4: 2/6 = 0.333
  Item3: 1/6 = 0.167
```

**Implementation:**

```java
@Component
public class RankingBasedAggregation implements AggregationStrategy {
    
    @Override
    public String getAggregationName() {
        return "ranking-based-borda-count";
    }
    
    @Override
    public List<ScoredItem> aggregate(
            Map<String, List<ScoredItem>> strategyResults,
            RecommendationContext context) {
        
        // Collect all unique items
        Map<Long, Item> allItems = new HashMap<>();
        strategyResults.values().forEach(results -> 
            results.forEach(scored -> 
                allItems.put(scored.getItem().getId(), scored.getItem())));
        
        // Compute Borda count for each item
        Map<Long, Integer> bordaCounts = new HashMap<>();
        int maxPossiblePoints = 0;
        
        for (Map.Entry<String, List<ScoredItem>> entry : strategyResults.entrySet()) {
            List<ScoredItem> results = entry.getValue();
            int N = results.size();
            maxPossiblePoints += N; // Maximum points from this strategy
            
            // Assign Borda points based on rank
            for (int rank = 0; rank < results.size(); rank++) {
                ScoredItem scored = results.get(rank);
                Long itemId = scored.getItem().getId();
                int points = N - rank; // Rank starts at 0, so N-rank gives N for rank 0
                
                bordaCounts.merge(itemId, points, Integer::sum);
            }
        }
        
        // Normalize scores to [0, 1]
        final double maxPoints = (double) maxPossiblePoints;
        
        return allItems.entrySet().stream()
            .map(entry -> {
                Long itemId = entry.getKey();
                Item item = entry.getValue();
                int points = bordaCounts.getOrDefault(itemId, 0);
                double normalizedScore = points / maxPoints;
                
                return ScoredItem.builder()
                    .item(item)
                    .score(normalizedScore)
                    .explanation("Consensus recommendation from " + 
                        strategyResults.size() + " strategies")
                    .sourceStrategy(getAggregationName())
                    .build();
            })
            .filter(scored -> scored.getScore() > 0)
            .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
            .collect(Collectors.toList());
    }
}
```

---

## Shared Utility Components

### SimilarityComputer

**Purpose:** Compute cosine similarity between sparse vectors.

```java
@Component
public class SimilarityComputer {
    
    /**
     * Computes cosine similarity between two sparse vectors.
     * 
     * @param vector1 Map of ID -> value
     * @param vector2 Map of ID -> value
     * @return Similarity score in [0, 1]
     */
    public double cosineSimilarity(
            Map<Long, Double> vector1,
            Map<Long, Double> vector2) {
        
        if (vector1.isEmpty() || vector2.isEmpty()) {
            return 0.0;
        }
        
        // Compute dot product
        double dotProduct = 0.0;
        for (Map.Entry<Long, Double> entry : vector1.entrySet()) {
            Long key = entry.getKey();
            if (vector2.containsKey(key)) {
                dotProduct += entry.getValue() * vector2.get(key);
            }
        }
        
        // Compute norms
        double norm1 = Math.sqrt(vector1.values().stream()
            .mapToDouble(v -> v * v)
            .sum());
        
        double norm2 = Math.sqrt(vector2.values().stream()
            .mapToDouble(v -> v * v)
            .sum());
        
        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }
        
        return dotProduct / (norm1 * norm2);
    }
}
```

---

## Configuration & Spring Integration

### Application Properties

```properties
# Recommendation Strategy Settings
recommendation.content.tfidf.enabled=true
recommendation.item-cf.top-k=50
recommendation.user-cf.top-k=30
recommendation.user-cf.min-interactions=5
recommendation.mf.latent-factors=50

# Aggregation Settings
recommendation.aggregation.type=constant

# Constant Convex Combination Weights
recommendation.weights.popularity=0.15
recommendation.weights.content=0.25
recommendation.weights.item-collaborative=0.25
recommendation.weights.user-collaborative=0.25
recommendation.weights.matrix-factorization=0.10
```

### Strategy Configuration

```java
@Configuration
public class RecommendationStrategyConfig {
    
    @Bean
    @Order(1)
    public RecommendationStrategy popularityStrategy() {
        return new PopularityStrategy();
    }
    
    @Bean
    @Order(2)
    public RecommendationStrategy contentBasedStrategy() {
        return new ContentBasedStrategy();
    }
    
    @Bean
    @Order(3)
    public RecommendationStrategy itemCollaborativeStrategy() {
        return new ItemBasedCollaborativeStrategy();
    }
    
    @Bean
    @Order(4)
    public RecommendationStrategy userCollaborativeStrategy() {
        return new UserBasedCollaborativeStrategy();
    }
    
    @Bean
    @Order(5)
    public RecommendationStrategy matrixFactorizationStrategy() {
        return new MatrixFactorizationStrategy();
    }
}
```

### Aggregation Configuration

```java
@Configuration
public class AggregationStrategyConfig {
    
    @Bean
    @Primary
    public AggregationStrategy defaultAggregation(
            @Value("${recommendation.aggregation.type:constant}") String type) {
        
        return switch(type) {
            case "constant" -> constantConvexAggregation();
            case "variable" -> variableConvexAggregation();
            case "ranking" -> rankingBasedAggregation();
            default -> constantConvexAggregation();
        };
    }
    
    @Bean
    public AggregationStrategy constantConvexAggregation() {
        return new ConstantConvexCombinationAggregation();
    }
    
    @Bean
    public AggregationStrategy variableConvexAggregation() {
        return new VariableConvexCombinationAggregation();
    }
    
    @Bean
    public AggregationStrategy rankingBasedAggregation() {
        return new RankingBasedAggregation();
    }
}
```

---

## Testing Strategy

### Unit Tests

**Per Strategy:**
- Test with mock providers and known data
- Verify scoring logic
- Test edge cases (empty data, cold start)
- Test normalization

**Per Aggregation:**
- Test weight normalization
- Test handling of missing items
- Test score computation with known inputs
- Test Borda count calculation

**Per DataProvider:**
- Test data computation logic
- Test caching behavior
- Test with various dataset sizes

### Integration Tests

```java
@SpringBootTest
@ActiveProfiles("test")
class RecommendationIntegrationTest {
    
    @Autowired
    private RecommendationEngine engine;
    
    @Test
    void testEndToEndRecommendation() {
        // Given: User with known preferences
        User user = createTestUser();
        RecommendationContext context = createTestContext();
        
        // When: Request recommendations
        List<ScoredItem> results = engine.recommend(user, context);
        
        // Then: Verify reasonable recommendations
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream()
            .allMatch(s -> s.getScore() >= 0 && s.getScore() <= 1));
    }
    
    @Test
    void testStrategyFailureGracefulDegradation() {
        // Test that engine continues with other strategies
        // when one strategy fails
    }
}
```

### Performance Tests

- Measure strategy execution time (target < 5 seconds each)
- Test with various dataset sizes
- Test similarity matrix computation time
- Test SVD training time
- Profile memory usage

---

## Implementation Roadmap

### Phase 1: Foundation (2-3 weeks)

#### Issue #1: Implement Popularity Baseline Strategy
**Scope:**
- Implement PopularityStrategy class
- Implement ItemPopularityProvider
- Add normalization logic
- Unit tests for strategy and provider
- Integration with engine

**Acceptance Criteria:**
- Strategy returns normalized TMDB popularity scores
- Works with empty and populated catalogs
- Properly cached (24 hours)

---

#### Issue #2: Implement Content-Based Strategy
**Scope:**
- Implement ContentBasedStrategy class
- Implement ItemFeatureVectorProvider
- Implement UserProfileProvider
- Add TF-IDF weighting
- Add cosine similarity computation
- Unit tests

**Acceptance Criteria:**
- Builds correct feature vectors for items
- Builds correct user profiles from history
- Computes similarity correctly
- Handles cold start users

---

#### Issue #3: Implement Constant Convex Combination Aggregation
**Scope:**
- Implement ConstantConvexCombinationAggregation class
- Add weight configuration
- Handle missing scores
- Unit tests
- Integration tests with 2 strategies

**Acceptance Criteria:**
- Correctly combines scores with configured weights
- Normalizes weights when strategies missing
- Produces sorted, deduplicated results
- **Milestone: MVP working system**

---

### Phase 2: Collaborative Filtering (3-4 weeks)

#### Issue #4: Implement SimilarityComputer Utility
**Scope:**
- Implement cosine similarity computation
- Add comprehensive unit tests
- Performance optimization for sparse vectors

**Acceptance Criteria:**
- Correctly computes cosine similarity
- Handles edge cases (empty vectors, no overlap)
- Performs efficiently on large sparse vectors

---

#### Issue #5: Implement Item-Based Collaborative Filtering
**Scope:**
- Implement ItemBasedCollaborativeStrategy
- Implement ItemSimilarityMatrixProvider
- Add top-K filtering
- Add caching (1 week expiration)
- Unit tests

**Acceptance Criteria:**
- Computes item similarities correctly
- Returns relevant recommendations
- Caches similarity matrix
- Handles cold start items

---

#### Issue #6: Implement User-Based Collaborative Filtering
**Scope:**
- Implement UserBasedCollaborativeStrategy
- Implement UserSimilarityProvider
- Reuse SimilarityComputer
- Add user similarity caching (1 hour)
- Unit tests

**Acceptance Criteria:**
- Computes user similarities correctly
- Returns relevant recommendations
- Falls back gracefully for new users
- Caches user similarities

---

#### Issue #7: Implement Variable Convex Combination Aggregation
**Scope:**
- Implement VariableConvexCombinationAggregation
- Add user maturity level detection
- Configure weight matrices for each level
- Unit tests

**Acceptance Criteria:**
- Correctly detects user maturity level
- Applies appropriate weights
- Smooth transitions between levels
- **Milestone: Personalized recommendations**

---

### Phase 3: Advanced Algorithms (2-3 weeks)

#### Issue #8: Implement Matrix Factorization Strategy
**Scope:**
- Implement MatrixFactorizationStrategy
- Implement MatrixFactorizationProvider
- Add SVD computation using Apache Commons Math
- Add weekly retraining job
- Store factored matrices
- Unit tests

**Acceptance Criteria:**
- Performs SVD correctly
- Generates accurate predictions
- Caches factors (1 week)
- Handles large sparse matrices

---

#### Issue #9: Implement Ranking-Based Aggregation (Borda Count)
**Scope:**
- Implement RankingBasedAggregation
- Add Borda count computation
- Unit tests with known rankings
- Integration tests

**Acceptance Criteria:**
- Correctly computes Borda points
- Normalizes scores to [0, 1]
- Handles varying strategy result sizes
- **Milestone: Complete system**

---

#### Issue #10: Integration Testing & Performance Optimization
**Scope:**
- End-to-end integration tests
- Performance benchmarking
- Optimization of slow components
- Load testing
- Documentation updates

**Acceptance Criteria:**
- All strategies work together
- Performance meets targets (< 10s total)
- Memory usage acceptable
- Comprehensive test coverage

---

## Summary

This design provides:

✅ **5 Recommendation Strategies** - Popularity, Content-Based, Item CF, User CF, Matrix Factorization  
✅ **3 Aggregation Methods** - Constant, Variable, Ranking-based (Borda Count)  
✅ **Agnostic Architecture** - DataProvider pattern keeps strategies independent  
✅ **Reusable Utilities** - SimilarityComputer shared across strategies  
✅ **Configurable System** - All parameters externalized  
✅ **Sequential Implementation** - Progressive delivery with early wins  
✅ **Comprehensive Testing** - Unit, integration, and performance tests  
✅ **Caching Strategy** - Optimized data provider caching  

**Total Estimated Time:** 8-12 weeks  
**Implementation Order:** Sequential (Foundation → Collaborative → Advanced)  
**First Milestone:** Working MVP after Issue #3 (3 weeks)  
**Final Milestone:** Complete system after Issue #10 (12 weeks)

---

**Next Steps:**
1. Create GitHub issues based on roadmap
2. Begin implementation with Issue #1 (Popularity Strategy)
3. Validate architecture with first strategy
4. Iterate and refine based on learnings
