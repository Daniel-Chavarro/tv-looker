# Recommendation Pipeline Refactoring: Async & Paginated Execution

## Objective
Optimize the `HybridRecommendationEngine` to handle large datasets efficiently. The current implementation loads all `Item`, `User`, and `Interaction` domain models into memory simultaneously, causing high memory pressure and slow response times. The refactor introduces asynchronous execution, paginated data loading, and a local-to-global "tournament" aggregation model.

## Core Architecture

### 1. Decoupling Context and Data Providers (Memory Optimization)
The `RecommendationContext` will no longer hold the entire dataset via massive `getAll()` calls. It will only contain the target `User`, active parameters, and the data cache.

`DataProvider` implementations (e.g., `MatrixFactorizationProvider`, `ItemFeatureVectorProvider`) will be updated to:
- Inject domain Services (e.g., `ItemService`, `InteractionService`).
- Fetch domain models using **paginated database queries** to build their global mathematical models (e.g., TF-IDF weights, SVD latent factors).
- Continue to cache these global models in memory with their configured TTL (e.g., 24 hours), separating the heavy global model training from the real-time scoring process.
- Strictly operate on Domain Models (`Item`, `User`) to preserve the domain-persistence boundary.

### 2. Async Chunked Scoring (Performance Optimization)
The engine's `recommend()` method will implement a chunked scoring pipeline:
- **PaginationSupplier:** The engine will fetch candidate items in pages (e.g., chunks of 100 items) via the `ItemService`.
- **Parallel Processing:** For each chunk, the engine submits an asynchronous task to a dedicated thread pool (`ExecutorService` / `CompletableFuture`).
- **Chunk Scoring:** Within the async task, the `RecommendationStrategy` implementations (Content-Based, Matrix Factorization, etc.) will score the 100 items using the pre-computed global models available in the `RecommendationContext`.

### 3. Two-Tier Borda Count Tournament (Aggregation)
To reduce memory overhead of holding scored items:
- **Local Tournament:** Immediately after scoring a chunk, the async task runs a local `AggregationStrategy` (Borda Count via `RankingBasedAggregation`) to determine the top N "representative" items for that specific page. Uncompetitive items are discarded.
- **Global Tournament:** The main thread waits for all async tasks to complete, aggregates the top representatives from all pages into a final pool, and runs one final Global Borda Count aggregation to rank the absolute best recommendations to return to the user.

## Component Modifications
- **`RecommendationContext`:** Remove bulk `List<Item>`, `List<User>` fields. Add methods to facilitate passing the target User and the cache.
- **`DataProvider` classes:** Replace context-data-fetching with paginated Domain Service calls.
- **`HybridRecommendationEngine`:** Implement the chunk fetching, `CompletableFuture` dispatching, and the two-tier aggregation logic.
- **Domain Services (`ItemService` etc.):** Expose paginated endpoints (returning Domain Models) if not already present.

## Error Handling & Edge Cases
- Ensure thread pool limits and timeouts are configured for the `CompletableFuture` execution to prevent hung threads.
- If a chunk fails, log the error and allow the engine to proceed with the remaining successful chunks to ensure a resilient recommendation response.
