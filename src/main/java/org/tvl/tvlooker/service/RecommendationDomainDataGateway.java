package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.dto.Review;
import org.tvl.tvlooker.domain.motor.utils.DataPage;
import org.tvl.tvlooker.domain.motor.utils.RecommendationDataGateway;

@Service
@RequiredArgsConstructor
public class RecommendationDomainDataGateway implements RecommendationDataGateway {

    private final ItemService itemService;
    private final InteractionService interactionService;
    private final ReviewService reviewService;

    @Override
    public DataPage<Item> getCandidateItems(int pageNumber, int pageSize) {
        return toDataPage(itemService.getAll(PageRequest.of(pageNumber, pageSize)));
    }

    @Override
    public DataPage<Item> getAllItems(int pageNumber, int pageSize) {
        return getCandidateItems(pageNumber, pageSize);
    }

    @Override
    public DataPage<Interaction> getAllInteractions(int pageNumber, int pageSize) {
        return toDataPage(interactionService.getAll(PageRequest.of(pageNumber, pageSize)));
    }

    @Override
    public DataPage<Review> getAllReviews(int pageNumber, int pageSize) {
        return toDataPage(reviewService.getAll(PageRequest.of(pageNumber, pageSize)));
    }

    private static <T> DataPage<T> toDataPage(Page<T> page) {
        return new DataPage<>(page.getContent(), page.hasNext());
    }
}
