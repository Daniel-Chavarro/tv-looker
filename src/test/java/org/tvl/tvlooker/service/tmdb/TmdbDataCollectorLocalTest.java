package org.tvl.tvlooker.service.tmdb;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.tvl.tvlooker.persistence.repository.ActorRepository;
import org.tvl.tvlooker.persistence.repository.DirectorRepository;
import org.tvl.tvlooker.persistence.repository.GenreRepository;
import org.tvl.tvlooker.persistence.repository.ItemRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class TmdbDataCollectorLocalTest {
    @Autowired
    private TmdbDataCollectorService collector;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private ActorRepository actorRepository;
    @Autowired
    private DirectorRepository directorRepository;

    @AfterEach
    @BeforeEach
    void cleanDb() {
        itemRepository.deleteAll();
        genreRepository.deleteAll();
        actorRepository.deleteAll();
        directorRepository.deleteAll();
    }


    @Test
    void testNoDuplicatesWithRealCollector() {
        collector.collectAll();
        long items1 = itemRepository.count();
        long genres1 = genreRepository.count();

        collector.collectAll();
        long items2 = itemRepository.count();
        long genres2 = genreRepository.count();

        assertEquals(items1, items2, "No debe haber duplicados en items");
        assertEquals(genres1, genres2, "No debe haber duplicados en géneros");
    }
}
