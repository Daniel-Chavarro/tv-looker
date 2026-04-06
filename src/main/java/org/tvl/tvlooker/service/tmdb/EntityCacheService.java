package org.tvl.tvlooker.service.tmdb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.persistence.repository.ActorRepository;
import org.tvl.tvlooker.persistence.repository.DirectorRepository;
import org.tvl.tvlooker.persistence.repository.GenreRepository;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbCastMemberMapper;
import org.tvl.tvlooker.persistence.tmdb.mapper.TmdbGenreMapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles batch caching and creation of entities to prevent N+1 queries.
 * <p>
 * Instead of querying the database for each actor/director/genre individually,
 * this service collects all unique entities from a batch of items and loads
 * them in bulk using a single query per entity type.
 * <p>
 * Example:
 * - Old: 100 items × 5 actors each = 500 queries
 * - New: 1 bulk query for 500 actors = 1 query total
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-04-03
 */
@Service
@Slf4j
public class EntityCacheService {

    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;
    private final GenreRepository genreRepository;

    public EntityCacheService(
            ActorRepository actorRepository,
            DirectorRepository directorRepository,
            GenreRepository genreRepository) {
        this.actorRepository = actorRepository;
        this.directorRepository = directorRepository;
        this.genreRepository = genreRepository;
    }

    /**
     * Batch finds or creates actors from TMDB cast members.
     * <p>
     * Strategy:
     * 1. Extract all unique TMDB actor IDs<p>
     * 2. Bulk query database for existing actors<p>
     * 3. Identify missing actors<p>
     * 4. Create and save missing actors<p>
     * 5. Return map of tmdbId → ActorEntity
     *
     * @param castMembers TMDB cast members (from credits response)
     * @return Map of tmdbId → ActorEntity (includes newly created)
     */
    @Transactional
    public Map<Long, ActorEntity> findOrCreateActors(List<TmdbCreditsDto.CastMember> castMembers) {
        if (castMembers == null || castMembers.isEmpty()) {
            return Map.of();
        }

        Set<Long> tmdbIds = castMembers.stream()
                .map(TmdbCreditsDto.CastMember::id)
                .collect(Collectors.toSet());

        log.debug("Finding or creating {} actors", tmdbIds.size());

        // 1. Bulk query existing actors
        List<ActorEntity> existing = actorRepository.findAllByTmdbIdIn(tmdbIds);
        Map<Long, ActorEntity> actorMap = existing.stream()
                .collect(Collectors.toMap(ActorEntity::getTmdbId, a -> a));

        // 2. Identify missing actors
        Set<Long> missingIds = tmdbIds.stream()
                .filter(id -> !actorMap.containsKey(id))
                .collect(Collectors.toSet());

        // 3. Create and save missing actors
        if (!missingIds.isEmpty()) {
            List<ActorEntity> newActors = castMembers.stream()
                    .filter(cast -> missingIds.contains(cast.id()))
                    .map(TmdbCastMemberMapper::toEntity)
                    .toList();

            List<ActorEntity> saved = actorRepository.saveAll(newActors);
            saved.forEach(a -> actorMap.put(a.getTmdbId(), a));

            log.debug("Created {} new actors", newActors.size());
        }

        return actorMap;
    }

    /**
     * Batch finds or creates directors from TMDB crew members.
     *
     * @param crewMembers TMDB crew members (from credits response)
     * @return Map of tmdbId → DirectorEntity
     */
    @Transactional
    public Map<Long, DirectorEntity> findOrCreateDirectors(List<TmdbCreditsDto.CrewMember> crewMembers) {
        if (crewMembers == null || crewMembers.isEmpty()) {
            return Map.of();
        }

        // Filter only directors
        List<TmdbCreditsDto.CrewMember> directors = crewMembers.stream()
                .filter(c -> "Director".equalsIgnoreCase(c.job()))
                .toList();

        if (directors.isEmpty()) {
            return Map.of();
        }

        Set<Long> tmdbIds = directors.stream()
                .map(TmdbCreditsDto.CrewMember::id)
                .collect(Collectors.toSet());

        log.debug("Finding or creating {} directors", tmdbIds.size());

        // Bulk query existing
        List<DirectorEntity> existing = directorRepository.findAllByTmdbIdIn(tmdbIds);
        Map<Long, DirectorEntity> directorMap = existing.stream()
                .collect(Collectors.toMap(DirectorEntity::getTmdbId, d -> d));

        // Create missing
        Set<Long> missingIds = tmdbIds.stream()
                .filter(id -> !directorMap.containsKey(id))
                .collect(Collectors.toSet());

        if (!missingIds.isEmpty()) {
            List<DirectorEntity> newDirectors = directors.stream()
                    .filter(crew -> missingIds.contains(crew.id()))
                    .map(TmdbCastMemberMapper::toEntity)
                    .toList();

            List<DirectorEntity> saved = directorRepository.saveAll(newDirectors);
            saved.forEach(d -> directorMap.put(d.getTmdbId(), d));

            log.debug("Created {} new directors", newDirectors.size());
        }

        return directorMap;
    }

    /**
     * Batch finds or creates genres from TMDB genre DTOs.
     *
     * @param genreDtos TMDB genre DTOs
     * @return Map of tmdbId → GenreEntity
     */
    @Transactional
    public Map<Long, GenreEntity> findOrCreateGenres(List<TmdbGenreDto> genreDtos) {
        if (genreDtos == null || genreDtos.isEmpty()) {
            return Map.of();
        }

        Set<Long> tmdbIds = genreDtos.stream()
                .map(g -> (long) g.id())
                .collect(Collectors.toSet());

        log.debug("Finding or creating {} genres", tmdbIds.size());

        // Bulk query existing
        List<GenreEntity> existing = genreRepository.findAllByTmdbIdIn(tmdbIds);
        Map<Long, GenreEntity> genreMap = existing.stream()
                .collect(Collectors.toMap(GenreEntity::getTmdbId, g -> g));

        // Create missing
        Set<Long> missingIds = tmdbIds.stream()
                .filter(id -> !genreMap.containsKey(id))
                .collect(Collectors.toSet());

        if (!missingIds.isEmpty()) {
            List<GenreEntity> newGenres = genreDtos.stream()
                    .filter(g -> missingIds.contains((long) g.id()))
                    .map(TmdbGenreMapper::toEntity)
                    .toList();

            List<GenreEntity> saved = genreRepository.saveAll(newGenres);
            saved.forEach(g -> genreMap.put(g.getTmdbId(), g));

            log.debug("Created {} new genres", newGenres.size());
        }

        return genreMap;
    }
}