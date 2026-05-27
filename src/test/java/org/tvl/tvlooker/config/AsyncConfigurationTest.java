package org.tvl.tvlooker.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringJUnitConfig(classes = AsyncConfiguration.class)
class AsyncConfigurationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldExposeRecommendationTaskExecutorBean() {
        Executor executor = applicationContext.getBean("recommendationTaskExecutor", Executor.class);

        assertNotNull(executor);
    }
}
