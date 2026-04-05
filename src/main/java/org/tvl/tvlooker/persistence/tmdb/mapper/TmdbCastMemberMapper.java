package org.tvl.tvlooker.persistence.tmdb.mapper;

import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.persistence.tmdb.dto.TmdbCreditsDto;

public final class TmdbCastMemberMapper {

    private TmdbCastMemberMapper() {
    }

    public static ActorEntity toEntity(TmdbCreditsDto.CastMember castMember) {
        ActorEntity actor = new ActorEntity();
        actor.setTmdbId(castMember.id());
        actor.setName(castMember.name());
        return actor;
    }

    public static DirectorEntity toEntity(TmdbCreditsDto.CrewMember crewMember) {
        DirectorEntity director = new DirectorEntity();
        director.setTmdbId(crewMember.id());
        director.setName(crewMember.name());
        return director;
    }
}
