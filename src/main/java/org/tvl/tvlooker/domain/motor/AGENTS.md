# Recommendation Engine Knowledge

## OVERVIEW
Domain-only recommendation engine boundary: strategies produce scored items, aggregation merges results, providers cache derived data in `RecommendationContext`.

## STRUCTURE
```text
domain/motor/
|-- RecommendationEngine.java
|-- HybridRecommendationEngine.java
`-- utils/
    |-- DataProvider.java
    |-- RecommendationContext.java
    |-- ScoredItem.java
    `-- provider/
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Engine contract | `RecommendationEngine.java` | Main `recommend(user, context)` API |
| Engine implementation | `HybridRecommendationEngine.java` | Strategy loop, aggregation, dedupe |
| Strategy contracts | `../strategy/recommendation/` | `RecommendationStrategy`, `PopularityStrategy` |
| Aggregation contracts | `../strategy/aggregation/` | `AggregationStrategy`, `ConstantConvexAggregation` |
| Provider cache | `utils/DataProvider.java`, `utils/RecommendationContext.java` | Lazily computed shared data |
| Item scoring | `utils/ScoredItem.java` | Carries item, score, explanation/source |
| Wiring | `../../config/RecommendationConfig.java` | Enables strategies and weights via properties |

## CONVENTIONS
- Strategies should not access repositories directly; `RecommendationService` builds context from services and passes it into the engine.
- New strategies belong in `domain/strategy/recommendation/` and must be wired in `RecommendationConfig`.
- New aggregators belong in `domain/strategy/aggregation/`; update the switch in `RecommendationConfig`.
- Providers belong in `utils/provider/` and should be registered through the engine config when shared computed data is needed.
- Keep recommendation logic free of HTTP/JPA annotations; this is domain code.

## ANTI-PATTERNS
- Do not bypass `RecommendationContext` with direct persistence calls from strategies.
- Do not change aggregation weights without keeping them summing to 1.0; `ConstantConvexAggregation` validates this.
- Do not rely on `checkDataNotNull()` as full validation; it only guards users/items.
- Do not remove cold-start behavior from popularity tests without adding equivalent coverage.

## COMMANDS
```bash
mvn test -Dtest=HybridRecommendationEngineTest
mvn test -Dtest=PopularityStrategyTest
mvn test -Dtest=ConstantConvexAggregationTest
mvn test -Dtest=ItemPopularityProviderTest
```

## NOTES
- `HybridRecommendationEngine.java` contains a TODO about logging configuration; avoid expanding `System.out` usage.
- `RecommendationService` currently returns only items, not scores/explanations, even though the domain types carry them.
