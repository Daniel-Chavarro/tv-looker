package org.tvl.tvlooker.domain.data_structure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemFeatureVectorTest {

    @Test
    @DisplayName("Should calculate dot product correctly and normalize with cosine similarity")
    void testCosineSimilarity() {
        ItemFeatureVector v1 = ItemFeatureVector.builder().build();
        v1.getGenres().put("Action", 1.0);
        v1.getGenres().put("Comedy", 1.0);

        ItemFeatureVector v2 = ItemFeatureVector.builder().build();
        v2.getGenres().put("Action", 1.0);
        v2.getGenres().put("Comedy", 1.0);

        // cosine sim of identical vectors is 1.0
        assertEquals(1.0, v1.cosineSimilarity(v2), 0.001);

        ItemFeatureVector v3 = ItemFeatureVector.builder().build();
        v3.getGenres().put("Drama", 1.0);
        // Orthogonal vectors -> 0.0
        assertEquals(0.0, v1.cosineSimilarity(v3), 0.001);
    }

    @Test
    @DisplayName("Should add vector with weight")
    void testAddVectorWithWeight() {
        ItemFeatureVector target = ItemFeatureVector.builder().build();
        ItemFeatureVector source = ItemFeatureVector.builder().build();
        source.getGenres().put("Action", 2.0);

        target.addVectorWithWeight(source, 3.0);
        assertEquals(6.0, target.getGenres().get("Action"), 0.001);

        ItemFeatureVector source2 = ItemFeatureVector.builder().build();
        source2.getGenres().put("Action", 1.0);
        target.addVectorWithWeight(source2, -1.0);

        assertEquals(5.0, target.getGenres().get("Action"), 0.001);
    }
}

