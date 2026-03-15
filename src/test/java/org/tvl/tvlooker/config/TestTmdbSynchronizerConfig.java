package org.tvl.tvlooker.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.tvl.tvlooker.service.tmdb.TmdbDataSynchronizerService;

/**
 * Test configuration to provide a mock TmdbDataSynchronizerService
 * when running with the "test" profile.
 */
@Configuration
@Profile("test")
public class TestTmdbSynchronizerConfig {

    @Bean
    @Primary
    public TmdbDataSynchronizerService tmdbDataSynchronizerService() {
        return Mockito.mock(TmdbDataSynchronizerService.class);
    }
}
