package org.tvl.tvlooker.domain.motor.utils.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.data_structure.ItemFeatureVector;
import org.tvl.tvlooker.domain.model.dto.ActorItem;
import org.tvl.tvlooker.domain.model.dto.Director;
import org.tvl.tvlooker.domain.model.dto.Genre;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.motor.utils.DataProvider;
import org.tvl.tvlooker.domain.motor.utils.RecommendationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes TF-IDF weighted feature vectors for items.
 *
 * Provider ID: "item-feature-vectors"
 * Cache: 24h
 *
 * Note: Tags are not processed in this implementation as the data model
 * does not yet include a Tag entity. The ItemFeatureVector structure
 * supports tags for future implementation when the data model is extended.
 */
@Component
public class ItemFeatureVectorProvider implements DataProvider<Map<Long, ItemFeatureVector>> {

    private static final Logger logger = LoggerFactory.getLogger(ItemFeatureVectorProvider.class);
    private final boolean enabled;

    public ItemFeatureVectorProvider(@Value("${recommendation.content.tfidf.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getProviderId() {
        return "item-feature-vectors";
    }

    @Override
    public long getCacheExpirationSeconds() {
        return 86400; // 24 hours
    }

    @Override
    public Map<Long, ItemFeatureVector> provide(RecommendationContext context) {
        Map<Long, ItemFeatureVector> vectors = new HashMap<>();

        if (!enabled) {
            logger.debug("TF-IDF content provider is disabled");
            return vectors;
        }

        List<Item> items = context.getItems();
        if (items == null || items.isEmpty()) {
            return vectors;
        }

        int totalItems = items.size();

        // Calculate document frequencies (DF)
        Map<String, Integer> genreDf = new HashMap<>();
        Map<String, Integer> actorDf = new HashMap<>();
        Map<String, Integer> directorDf = new HashMap<>();

        for (Item item : items) {
            if (item.getGenres() != null) {
                for (Genre g : item.getGenres()) {
                    if (g.getName() != null && !g.getName().isBlank()) {
                        genreDf.put(g.getName(), genreDf.getOrDefault(g.getName(), 0) + 1);
                    }
                }
            }
            if (item.getActorsInItem() != null) {
                for (ActorItem a : item.getActorsInItem()) {
                    if (a.getActor() != null && a.getActor().getName() != null && !a.getActor().getName().isBlank()) {
                        actorDf.put(a.getActor().getName(), actorDf.getOrDefault(a.getActor().getName(), 0) + 1);
                    }
                }
            }
            if (item.getDirectors() != null) {
                for (Director d : item.getDirectors()) {
                    if (d.getName() != null && !d.getName().isBlank()) {
                        directorDf.put(d.getName(), directorDf.getOrDefault(d.getName(), 0) + 1);
                    }
                }
            }
        }

        // Calculate TF-IDF vectors
        for (Item item : items) {
            if (item.getId() == null) {
                continue;
            }

            ItemFeatureVector.ItemFeatureVectorBuilder vectorBuilder = ItemFeatureVector.builder();
            ItemFeatureVector vector = vectorBuilder.build(); // Using default empty maps

            if (item.getGenres() != null) {
                for (Genre g : item.getGenres()) {
                    if (g.getName() != null && !g.getName().isBlank()) {
                        double idf = Math.log((double) totalItems / (1 + genreDf.getOrDefault(g.getName(), 0)));
                        vector.getGenres().put(g.getName(), idf);
                    }
                }
            }

            if (item.getActorsInItem() != null) {
                for (ActorItem a : item.getActorsInItem()) {
                    if (a.getActor() != null && a.getActor().getName() != null && !a.getActor().getName().isBlank()) {
                        double idf = Math.log((double) totalItems / (1 + actorDf.getOrDefault(a.getActor().getName(), 0)));
                        vector.getActors().put(a.getActor().getName(), idf);
                    }
                }
            }

            if (item.getDirectors() != null) {
                for (Director d : item.getDirectors()) {
                    if (d.getName() != null && !d.getName().isBlank()) {
                        double idf = Math.log((double) totalItems / (1 + directorDf.getOrDefault(d.getName(), 0)));
                        vector.getDirectors().put(d.getName(), idf);
                    }
                }
            }

            vectors.put(item.getId(), vector);
        }

        return vectors;
    }
}
