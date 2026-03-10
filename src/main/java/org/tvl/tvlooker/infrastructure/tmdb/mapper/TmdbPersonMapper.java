package org.tvl.tvlooker.infrastructure.tmdb.mapper;

import org.tvl.tvlooker.domain.model.entity.Actor;
import org.tvl.tvlooker.domain.model.entity.Director;
import org.tvl.tvlooker.infrastructure.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.repository.ActorRepository;
import org.tvl.tvlooker.repository.DirectorRepository;

/**
 * Maps TMDB cast/crew members to Actor and Director JPA entities using find-or-create by tmdbId.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-10
 */
public final class TmdbPersonMapper {

    private TmdbPersonMapper() {
        // Utility class
    }

    /**
     * Finds an existing Actor by tmdbId or creates and saves a new one.
     *
     * @param castMember the TMDB cast member
     * @param repository the actor repository
     * @return the existing or newly created Actor entity
     */
    public static Actor findOrCreateActor(
            TmdbCreditsDto.CastMember castMember,
            ActorRepository repository) {
        return repository.findByTmdbId(castMember.id())
                .orElseGet(() -> {
                    Actor actor = new Actor();
                    actor.setTmdbId(castMember.id());
                    actor.setName(castMember.name());
                    return repository.save(actor);
                });
    }

    /**
     * Finds an existing Director by tmdbId or creates and saves a new one.
     * Only crew members with job="Director" should be passed here.
     *
     * @param crewMember the TMDB crew member
     * @param repository the director repository
     * @return the existing or newly created Director entity
     */
    public static Director findOrCreateDirector(
            TmdbCreditsDto.CrewMember crewMember,
            DirectorRepository repository) {
        return repository.findByTmdbId(crewMember.id())
                .orElseGet(() -> {
                    Director director = new Director();
                    director.setTmdbId(crewMember.id());
                    director.setName(crewMember.name());
                    return repository.save(director);
                });
    }
}

