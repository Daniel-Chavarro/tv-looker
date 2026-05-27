package org.tvl.tvlooker.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.model.dto.Review;
import org.tvl.tvlooker.domain.motor.utils.DataPage;
import org.tvl.tvlooker.testutil.TestDataFactory;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationDomainDataGateway tests")
class RecommendationDomainDataGatewayTest {

    @Mock
    private ItemService itemService;

    @Mock
    private InteractionService interactionService;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private RecommendationDomainDataGateway gateway;

    @Test
    @DisplayName("getCandidateItems should delegate to ItemService pagination")
    void getCandidateItemsShouldDelegateToItemServicePagination() {
        List<Item> items = TestDataFactory.createItems(2);
        Pageable pageable = PageRequest.of(1, 2);
        when(itemService.getAll(pageable)).thenReturn(new PageImpl<>(items, pageable, 5));

        DataPage<Item> result = gateway.getCandidateItems(1, 2);

        assertThat(result.content()).containsExactlyElementsOf(items);
        assertThat(result.hasNext()).isTrue();
        verify(itemService).getAll(eq(pageable));
    }

    @Test
    @DisplayName("getAllItems should delegate to ItemService pagination")
    void getAllItemsShouldDelegateToItemServicePagination() {
        List<Item> items = TestDataFactory.createItems(1);
        Pageable pageable = PageRequest.of(0, 10);
        when(itemService.getAll(pageable)).thenReturn(new PageImpl<>(items, pageable, 1));

        DataPage<Item> result = gateway.getAllItems(0, 10);

        assertThat(result.content()).containsExactlyElementsOf(items);
        assertThat(result.hasNext()).isFalse();
        verify(itemService).getAll(eq(pageable));
    }

    @Test
    @DisplayName("getAllInteractions should delegate to InteractionService pagination")
    void getAllInteractionsShouldDelegateToInteractionServicePagination() {
        Interaction interaction = TestDataFactory.createWatchInteraction(1L, UUID.randomUUID(), 1L);
        Pageable pageable = PageRequest.of(0, 25);
        when(interactionService.getAll(pageable)).thenReturn(new PageImpl<>(List.of(interaction), pageable, 1));

        DataPage<Interaction> result = gateway.getAllInteractions(0, 25);

        assertThat(result.content()).containsExactly(interaction);
        assertThat(result.hasNext()).isFalse();
        verify(interactionService).getAll(eq(pageable));
    }

    @Test
    @DisplayName("getAllReviews should delegate to ReviewService pagination")
    void getAllReviewsShouldDelegateToReviewServicePagination() {
        Review review = new Review(1L, null, 1L, 5, null, null);
        Pageable pageable = PageRequest.of(0, 50);
        when(reviewService.getAll(pageable)).thenReturn(new PageImpl<>(List.of(review), pageable, 1));

        DataPage<Review> result = gateway.getAllReviews(0, 50);

        assertThat(result.content()).containsExactly(review);
        assertThat(result.hasNext()).isFalse();
        verify(reviewService).getAll(eq(pageable));
    }
}
