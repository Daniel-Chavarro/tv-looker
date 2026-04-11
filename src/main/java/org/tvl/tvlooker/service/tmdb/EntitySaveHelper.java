package org.tvl.tvlooker.service.tmdb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.persistence.repository.ActorRepository;
import org.tvl.tvlooker.persistence.repository.DirectorRepository;
import org.tvl.tvlooker.persistence.repository.GenreRepository;

@Service
@Slf4j
public class EntitySaveHelper {

    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;
    private final GenreRepository genreRepository;

    public EntitySaveHelper(
            ActorRepository actorRepository,
            DirectorRepository directorRepository,
            GenreRepository genreRepository) {
        this.actorRepository = actorRepository;
        this.directorRepository = directorRepository;
        this.genreRepository = genreRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ActorEntity saveActor(ActorEntity actor) {
        return actorRepository.saveAndFlush(actor);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DirectorEntity saveDirector(DirectorEntity director) {
        return directorRepository.saveAndFlush(director);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GenreEntity saveGenre(GenreEntity genre) {
        return genreRepository.saveAndFlush(genre);
    }
}