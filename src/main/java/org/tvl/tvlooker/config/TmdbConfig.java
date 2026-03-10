package org.tvl.tvlooker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

/**
 * Configuration class for the TMDB API connection.
 * Creates a pre-configured RestClient bean with the base URL and authentication header.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-10
 */
@Configuration
@EnableScheduling
public class TmdbConfig {

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.base-url:https://api.themoviedb.org/3}")
    private String baseUrl;

    /**
     * Creates a RestClient pre-configured for the TMDB API.
     * Includes the Bearer token for authentication and JSON accept header.
     *
     * <p>Usage example from any Spring-managed class:</p>
     * <pre>
     * {@code
     * @Autowired
     * private RestClient tmdbRestClient;
     *
     * String response = tmdbRestClient.get()
     *         .uri("/movie/popular?language=es-MX&page=1")
     *         .retrieve()
     *         .body(String.class);
     * }
     * </pre>
     *
     * @return a RestClient configured for the TMDB API v3
     */
    @Bean
    public RestClient tmdbRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}



