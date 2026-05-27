package org.tvl.tvlooker.domain.motor.utils;

import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.dto.Review;

public interface RecommendationDataGateway {

    DataPage<Item> getCandidateItems(int pageNumber, int pageSize);

    DataPage<Item> getAllItems(int pageNumber, int pageSize);

    DataPage<Interaction> getAllInteractions(int pageNumber, int pageSize);

    DataPage<Review> getAllReviews(int pageNumber, int pageSize);
}
