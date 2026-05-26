package org.tvl.tvlooker.domain.data_structure;

import lombok.Builder;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an item or user profile as a vector of features.
 * Features are separated by type (genres, actors, directors, tags)
 * to avoid collisions and allow different weighting if needed.
 */
@Getter
@Builder
public class ItemFeatureVector {

    @Builder.Default
    private final Map<String, Double> genres = new HashMap<>();
    @Builder.Default
    private final Map<String, Double> actors = new HashMap<>();
    @Builder.Default
    private final Map<String, Double> directors = new HashMap<>();
    @Builder.Default
    private final Map<String, Double> tags = new HashMap<>();

    /**
     * Calculates the cosine similarity between this vector and another vector.
     * Similitud coseno: dotProduct / (norm1 * norm2)
     */
    public double cosineSimilarity(ItemFeatureVector other) {
        if (other == null) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double norm1Sq = 0.0;
        double norm2Sq = 0.0;

        dotProduct += dotProductMap(this.genres, other.genres);
        dotProduct += dotProductMap(this.actors, other.actors);
        dotProduct += dotProductMap(this.directors, other.directors);
        dotProduct += dotProductMap(this.tags, other.tags);

        norm1Sq += sumOfSquares(this.genres);
        norm1Sq += sumOfSquares(this.actors);
        norm1Sq += sumOfSquares(this.directors);
        norm1Sq += sumOfSquares(this.tags);

        norm2Sq += sumOfSquares(other.genres);
        norm2Sq += sumOfSquares(other.actors);
        norm2Sq += sumOfSquares(other.directors);
        norm2Sq += sumOfSquares(other.tags);

        double denom = Math.sqrt(norm1Sq) * Math.sqrt(norm2Sq);
        if (denom == 0.0) {
            return 0.0;
        }

        return dotProduct / denom;
    }

    private double dotProductMap(Map<String, Double> map1, Map<String, Double> map2) {
        double dot = 0.0;
        if (map1.size() < map2.size()) {
            for (Map.Entry<String, Double> entry : map1.entrySet()) {
                Double v2 = map2.get(entry.getKey());
                if (v2 != null) {
                    dot += entry.getValue() * v2;
                }
            }
        } else {
            for (Map.Entry<String, Double> entry : map2.entrySet()) {
                Double v1 = map1.get(entry.getKey());
                if (v1 != null) {
                    dot += v1 * entry.getValue();
                }
            }
        }
        return dot;
    }

    private double sumOfSquares(Map<String, Double> map) {
        double sum = 0.0;
        for (Double val : map.values()) {
            if (val != null) {
                sum += val * val;
            }
        }
        return sum;
    }

    /**
     * Appends another vector to this vector (used for generating user profiles),
     * scaling by a given weight.
     */
    public void addVectorWithWeight(ItemFeatureVector other, double weight) {
        if (other == null) {
            return;
        }
        addMapWithWeight(this.genres, other.genres, weight);
        addMapWithWeight(this.actors, other.actors, weight);
        addMapWithWeight(this.directors, other.directors, weight);
        addMapWithWeight(this.tags, other.tags, weight);
    }

    private void addMapWithWeight(Map<String, Double> target, Map<String, Double> source, double weight) {
        for (Map.Entry<String, Double> entry : source.entrySet()) {
            Double val = entry.getValue();
            if (val != null) {
                target.put(entry.getKey(), target.getOrDefault(entry.getKey(), 0.0) + (val * weight));
            }
        }
    }
}

