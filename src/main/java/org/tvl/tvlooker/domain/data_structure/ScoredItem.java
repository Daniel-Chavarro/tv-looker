package org.tvl.tvlooker.domain.data_structure;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tvl.tvlooker.domain.model.entity.Item;


/**
 * ScoredItem is a simple data structure that holds a User, a score, and an explanation for that score.
 * It is used to represent the result of a recommendation or evaluation process, where each User is assigned a score
 * based on certain criteria. The explanation field provides additional context for why the User received that
 * particular score.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ScoredItem {
    /** The recommended item */
    private Item item;

    /**
     * The score assigned to the item.
     * Range: 0.0 to 1.0, where higher scores indicate stronger recommendations.
     */
    private double score;

    /**
     * Human-readable explanation for why this item was recommended.
     * Example: "Users similar to you loved this", "Same genre as your favorites"
     */
    private String explanation;

    /**
     * Track which strategy generated this recommendation (optional, for debugging).
     * Set by strategies or during aggregation.
     */
    private String sourceStrategy;
}
