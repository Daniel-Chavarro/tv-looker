package org.tvl.tvlooker.domain.motor.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.testutil.TestDataFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecommendationDataGateway domain paging tests")
class RecommendationDataGatewayTest {

    @Test
    @DisplayName("DataPage should expose immutable page content and next-page flag")
    void dataPageShouldExposeContentAndNextPageFlag() {
        List<Item> items = TestDataFactory.createItems(2);

        DataPage<Item> page = new DataPage<>(items, true);

        assertThat(page.content()).containsExactlyElementsOf(items);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.isEmpty()).isFalse();
    }

    @Test
    @DisplayName("DataPage empty factory should return empty page without next page")
    void emptyFactoryShouldReturnEmptyPageWithoutNextPage() {
        DataPage<Item> page = DataPage.empty();

        assertThat(page.content()).isEmpty();
        assertThat(page.hasNext()).isFalse();
        assertThat(page.isEmpty()).isTrue();
    }
}
