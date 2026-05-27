package org.tvl.tvlooker.domain.motor.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tvl.tvlooker.testutil.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecommendationContext tests")
class RecommendationContextTest {

    @Test
    @DisplayName("builder should keep safe defaults for provider cache")
    void builderShouldKeepSafeDefaultsForProviderCache() {
        RecommendationContext context = RecommendationContext.builder().build();

        assertThat(context.getDataProviders()).isNotNull().isEmpty();
        assertThat(context.getDataCache()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("builder should populate page-aware fields")
    void builderShouldPopulatePageAwareFields() {
        RecommendationContext context = TestDataFactory.createPagedContext(
                null,
                TestDataFactory.createGateway(java.util.List.of(), java.util.List.of(), java.util.List.of()),
                25,
                5,
                1_500L);

        assertThat(context.getTargetUser()).isNull();
        assertThat(context.getDataGateway()).isNotNull();
        assertThat(context.getCandidatePageSize()).isEqualTo(25);
        assertThat(context.getRepresentativesPerPage()).isEqualTo(5);
        assertThat(context.getAsyncTimeoutMillis()).isEqualTo(1_500L);
        assertThat(context.checkDataNotNull()).isTrue();
    }

    @Test
    @DisplayName("gateway fixture should page deterministically")
    void gatewayFixtureShouldPageDeterministically() {
        var items = TestDataFactory.createItems(5);
        var gateway = TestDataFactory.createGateway(items, java.util.List.of(), java.util.List.of());

        assertThat(gateway.getAllItems(0, 2).content()).containsExactly(items.get(0), items.get(1));
        assertThat(gateway.getAllItems(1, 2).content()).containsExactly(items.get(2), items.get(3));
        assertThat(gateway.getAllItems(2, 2).content()).containsExactly(items.get(4));
        assertThat(gateway.getAllItems(3, 2)).isEqualTo(DataPage.empty());
    }
}
