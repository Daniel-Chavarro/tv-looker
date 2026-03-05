package org.tvl.tvlooker.domain.data_structure;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.tvl.tvlooker.domain.model.entity.User;

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
    /** The user associated with the scored item. This field holds a reference to a User object, which contains
     * information about the user such as their ID, username, and other relevant details. The score and explanation
     * fields provide additional information about the user's relevance or suitability for a particular recommendation
     * or evaluation context.
     */
    private User user;
    /** The score assigned to the user, represented as a double. This score is typically calculated based on various
     * factors such as user interactions, preferences, or other relevant data. The score can be used to rank or
     * compare users in the context of recommendations or evaluations. The score is a number between 0 and 1, where a
     * higher score indicates a stronger recommendation or relevance for the user.
     */
    private double score;

    /** The explanation for the assigned score, represented as a string. This field provides additional context or
     * reasoning behind why the user received a particular score. The explanation can include details about the
     * factors that contributed to the score, such as specific interactions, preferences, or other relevant data
     * that influenced the recommendation or evaluation process. This helps users understand the rationale behind
     * their score and can enhance transparency in the recommendation system.
     */
    private String explanation;
}
