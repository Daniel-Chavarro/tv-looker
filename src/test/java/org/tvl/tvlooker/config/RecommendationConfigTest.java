package org.tvl.tvlooker.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.tvl.tvlooker.domain.motor.HybridRecommendationEngine;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringJUnitConfig(classes = {AsyncConfiguration.class, RecommendationConfig.class})
class RecommendationConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldCreateRecommendationEngineWithDedicatedExecutor() {
        HybridRecommendationEngine engine = applicationContext.getBean(HybridRecommendationEngine.class);

        assertNotNull(engine);
        assertNotNull(readExecutorField(engine));
    }

    private Object readExecutorField(HybridRecommendationEngine engine) {
        try {
            Field field = HybridRecommendationEngine.class.getDeclaredField("RECOMMENDATION_TASK_EXECUTOR");
            field.setAccessible(true);
            return field.get(engine);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
