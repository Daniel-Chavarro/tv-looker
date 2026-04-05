package org.tvl.tvlooker.persistence.tmdb.mapper;


import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;
import org.tvl.tvlooker.persistence.repository.ActorRepository;
import org.tvl.tvlooker.persistence.repository.DirectorRepository;

/**
 * Maps TMDB cast/crew members to Actor and Director JPA entities using find-or-create by tmdbId.
 *
 * @author TV Looker Team
 * @version 1.0
 * @since 2026-03-10
 */
public final class TmdbCastMemberMapper {

    private TmdbCastMemberMapper() {
        // Utility class
    }

    /**
     * Finds an existing Actor by tmdbId or creates and saves a new one.
     *
     * @param castMember the TMDB cast member
     * @param repository the actor repository
     * @return the existing or newly created Actor entity
     */
    @Deprecated(forRemoval = true)
    public static ActorEntity findOrCreateActor(
            TmdbCreditsDto.CastMember castMember,
            ActorRepository repository) {
        return repository.findByTmdbId(castMember.id())
                .orElseGet(() -> {
                    ActorEntity actor = toEntity(castMember);
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
    @Deprecated(forRemoval = true)
    public static DirectorEntity findOrCreateDirector(
            TmdbCreditsDto.CrewMember crewMember,
            DirectorRepository repository) {
        return repository.findByTmdbId(crewMember.id())
                .orElseGet(() -> {
                    DirectorEntity director = toEntity(crewMember);
                    return repository.save(director);
                });
    }

    /**
     * Converts a TMDB cast member DTO to an Actor JPA entity.
     * @param castMember the TMDB cast member DTO
     * @return the Actor entity
     */
    public static ActorEntity toEntity(TmdbCreditsDto.CastMember castMember) {
        ActorEntity actor = new ActorEntity();
        actor.setTmdbId(castMember.id());
        actor.setName(castMember.name());
        return actor;
    }

    /**
     * Converts a TMDB crew member DTO to a Director JPA entity.
     * @param crewMember the TMDB crew member DTO (should have job="Director")
     * @return the Director entity
     */
    public static DirectorEntity toEntity(TmdbCreditsDto.CrewMember crewMember) {
        DirectorEntity director = new DirectorEntity();
        director.setTmdbId(crewMember.id());
        director.setName(crewMember.name());
        return director;
    }
}

