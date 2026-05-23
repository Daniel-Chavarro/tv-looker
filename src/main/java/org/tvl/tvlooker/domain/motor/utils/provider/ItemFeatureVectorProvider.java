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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
                Set<String> seen = new HashSet<>();
                for (Genre g : item.getGenres()) {
                    if (isValidName(g.getName()) && seen.add(g.getName())) {
                        genreDf.merge(g.getName(), 1, Integer::sum);
                    }
                }
            }
            if (item.getActorsInItem() != null) {
                Set<String> seen = new HashSet<>();
                for (ActorItem a : item.getActorsInItem()) {
                    String actorName = getActorName(a);
                    if (isValidName(actorName) && seen.add(actorName)) {
                        actorDf.merge(actorName, 1, Integer::sum);
                    }
                }
            }
            if (item.getDirectors() != null) {
                Set<String> seen = new HashSet<>();
                for (Director d : item.getDirectors()) {
                    if (isValidName(d.getName()) && seen.add(d.getName())) {
                        directorDf.merge(d.getName(), 1, Integer::sum);
                    }
                }
            }
        }

        // Calculate TF-IDF vectors
        for (Item item : items) {
            if (item.getId() == null) {
                continue;
            }

            ItemFeatureVector vector = ItemFeatureVector.builder().build();

            if (item.getGenres() != null) {
                for (Genre g : item.getGenres()) {
                    String name = g.getName();
                    if (isValidName(name)) {
                        double idf = Math.log((double) (totalItems + 1) / (1 + genreDf.getOrDefault(name, 0)));
                        vector.getGenres().put(name, idf);
                    }
                }
            }

            if (item.getActorsInItem() != null) {
                for (ActorItem a : item.getActorsInItem()) {
                    String actorName = getActorName(a);
                    if (isValidName(actorName)) {
                        double idf = Math.log((double) (totalItems + 1) / (1 + actorDf.getOrDefault(actorName, 0)));
                        vector.getActors().put(actorName, idf);
                    }
                }
            }

            if (item.getDirectors() != null) {
                for (Director d : item.getDirectors()) {
                    String name = d.getName();
                    if (isValidName(name)) {
                        double idf = Math.log((double) (totalItems + 1) / (1 + directorDf.getOrDefault(name, 0)));
                        vector.getDirectors().put(name, idf);
                    }
                }
            }

            vectors.put(item.getId(), vector);
        }

        return vectors;
    }

    private static boolean isValidName(String name) {
        return name != null && !name.isBlank();
    }

    private static String getActorName(ActorItem a) {
        if (a.getActor() == null) {
            return null;
        }
        return a.getActor().getName();
    }
}
