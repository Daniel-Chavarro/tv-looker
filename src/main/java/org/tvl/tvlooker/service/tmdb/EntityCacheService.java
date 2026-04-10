package org.tvl.tvlooker.service.tmdb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.persistence.repository.ActorRepository;
import org.tvl.tvlooker.persistence.repository.DirectorRepository;
import org.tvl.tvlooker.persistence.repository.GenreRepository;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbGenreDto;

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
    private final EntitySaveHelper entitySaveHelper;

    public EntityCacheService(
            ActorRepository actorRepository,
            DirectorRepository directorRepository,
            GenreRepository genreRepository,
            EntitySaveHelper entitySaveHelper) {
        this.actorRepository = actorRepository;
        this.directorRepository = directorRepository;
        this.genreRepository = genreRepository;
        this.entitySaveHelper = entitySaveHelper;
    }

    /**
     * Batch finds or creates actors from TMDB cast members.
     * <p>
     * Strategy:
     * 1. Extract all unique TMDB actor IDs<p>
     * 2. Bulk query database for existing actors<p>
     * 3. Identify missing actors<p>
     * 4. Create and save missing actors with duplicate handling for race conditions<p>
     * 5. Return map of tmdbId → ActorEntity
     *
     * @param castMembers TMDB cast members (from credits response)
     * @return Map of tmdbId → ActorEntity (includes newly created)
     */
    public Map<Long, ActorEntity> findOrCreateActors(List<TmdbCreditsDto.CastMember> castMembers) {
        if (castMembers == null || castMembers.isEmpty()) {
            return Map.of();
        }

        Set<Long> tmdbIds = castMembers.stream()
                .map(TmdbCreditsDto.CastMember::id)
                .collect(Collectors.toSet());

        log.debug("Finding or creating {} actors", tmdbIds.size());

        List<ActorEntity> existing = actorRepository.findAllByTmdbIdIn(tmdbIds);
        Map<Long, ActorEntity> actorMap = existing.stream()
                .collect(Collectors.toMap(ActorEntity::getTmdbId, a -> a));

        Set<Long> missingIds = tmdbIds.stream()
                .filter(id -> !actorMap.containsKey(id))
                .collect(Collectors.toSet());

        if (!missingIds.isEmpty()) {
            for (TmdbCreditsDto.CastMember cast : castMembers) {
                if (!missingIds.contains(cast.id())) {
                    continue;
                }

                ActorEntity actor;

                try{
                    actor = entitySaveHelper.saveActor(ActorEntity.builder()
                            .name(cast.name())
                            .tmdbId(cast.id())
                            .build());
                } catch (DataIntegrityViolationException e) {
                    actor = actorRepository.findByTmdbId(cast.id()).orElseThrow();
                }

                actorMap.put(cast.id(), actor);
                missingIds.remove(cast.id());
                log.debug("Upserted actor: tmdbId={}", cast.id());
            }

            log.debug("Upserted {} actors", missingIds.size());
        }

        return actorMap;
    }

    /**
     * Batch finds or creates directors from TMDB crew members.
     * <p>
     * Handles race conditions when multiple threads attempt to create the same director.
     *
     * @param crewMembers TMDB crew members (from credits response)
     * @return Map of tmdbId → DirectorEntity
     */
    public Map<Long, DirectorEntity> findOrCreateDirectors(List<TmdbCreditsDto.CrewMember> crewMembers) {
        if (crewMembers == null || crewMembers.isEmpty()) {
            return Map.of();
        }

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

        List<DirectorEntity> existing = directorRepository.findAllByTmdbIdIn(tmdbIds);
        Map<Long, DirectorEntity> directorMap = existing.stream()
                .collect(Collectors.toMap(DirectorEntity::getTmdbId, d -> d));

        Set<Long> missingIds = tmdbIds.stream()
                .filter(id -> !directorMap.containsKey(id))
                .collect(Collectors.toSet());

        if (!missingIds.isEmpty()) {
            for (TmdbCreditsDto.CrewMember crew : directors) {
                if (!missingIds.contains(crew.id())) {
                    continue;
                }
                DirectorEntity director;

                try {
                    director = entitySaveHelper.saveDirector(DirectorEntity.builder()
                            .name(crew.name())
                            .tmdbId(crew.id())
                            .build()
                    );
                } catch (DataIntegrityViolationException e) {
                    director = directorRepository.findByTmdbId(crew.id()).orElseThrow();
                }

                directorMap.put(crew.id(), director);
                missingIds.remove(crew.id());
                log.debug("Upserted director: tmdbId={}", crew.id());
            }

            log.debug("Upserted {} directors", missingIds.size());
        }

        return directorMap;
    }

    /**
     * Batch finds or creates genres from TMDB genre DTOs.
     * <p>
     * Handles race conditions when multiple threads attempt to create the same genre.
     *
     * @param genreDtos TMDB genre DTOs
     * @return Map of tmdbId → GenreEntity
     */
    public Map<Long, GenreEntity> findOrCreateGenres(List<TmdbGenreDto> genreDtos) {
        if (genreDtos == null || genreDtos.isEmpty()) {
            return Map.of();
        }

        Set<Long> tmdbIds = genreDtos.stream()
                .map(g -> (long) g.id())
                .collect(Collectors.toSet());

        log.debug("Finding or creating {} genres", tmdbIds.size());

        List<GenreEntity> existing = genreRepository.findAllByTmdbIdIn(tmdbIds);
        Map<Long, GenreEntity> genreMap = existing.stream()
                .collect(Collectors.toMap(GenreEntity::getTmdbId, g -> g));

        Set<Long> missingIds = tmdbIds.stream()
                .filter(id -> !genreMap.containsKey(id))
                .collect(Collectors.toSet());

        if (!missingIds.isEmpty()) {
            for (TmdbGenreDto genreDto : genreDtos) {
                long genreId = genreDto.id();
                if (!missingIds.contains(genreId)) {
                    continue;
                }

                GenreEntity genre;

                try {
                    genre = entitySaveHelper.saveGenre(GenreEntity.builder()
                            .name(genreDto.name())
                            .tmdbId((long) genreDto.id())
                            .build());
                } catch (DataIntegrityViolationException e) {
                    genre = genreRepository.findByTmdbId((long) genreDto.id()).orElseThrow();
                }

                genreMap.put((long) genreDto.id(), genre);
                missingIds.remove(genreId);
                log.debug("Upserted genre: tmdbId={}", genreId);
            }

            log.debug("Upserted {} genres", missingIds.size());
        }

        return genreMap;
    }
}