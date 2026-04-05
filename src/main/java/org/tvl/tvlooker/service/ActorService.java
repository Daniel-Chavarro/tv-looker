package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.model.Actor;
import org.tvl.tvlooker.domain.exception.ActorNotFoundException;
import org.tvl.tvlooker.domain.model.entity.ActorEntity;
import org.tvl.tvlooker.domain.model.mapper.ActorEntityMapper;
import org.tvl.tvlooker.persistence.repository.ActorRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActorService {

	private final ActorRepository actorRepository;

	/**
	 * Create a new actor.
	 *
	 * @param actor actor to persist
	 * @return saved actor
	 */
	public Actor create(Actor actor) {
		ActorEntity entity = ActorEntityMapper.toEntity(actor);
		return ActorEntityMapper.toDomain(actorRepository.save(entity));
	}

	/**
	 * Get an actor by id.
	 *
	 * @param id actor id
	 * @return actor
	 * @throws ActorNotFoundException when the actor does not exist
	 */
	public Actor getById(Long id) {
		return actorRepository.findById(id)
				.map(ActorEntityMapper::toDomain)
				.orElseThrow(() -> new ActorNotFoundException("Actor not found: " + id));
	}

	/**
	 * Get all actors.
	 *
	 * @return list of actors
	 */
	public List<Actor> getAll() {
		return actorRepository.findAll().stream()
				.map(ActorEntityMapper::toDomain)
				.collect(Collectors.toList());
	}

	/**
	 * Update an actor.
	 *
	 * @param id actor id
	 * @param actor actor data to update
	 * @return updated actor
	 * @throws ActorNotFoundException when the actor does not exist
	 */
	public Actor update(Long id, Actor actor) {
		if (!actorRepository.existsById(id)) {
			throw new ActorNotFoundException("Actor not found: " + id);
		}
		ActorEntity actual = actorRepository.getReferenceById(id);
		ActorEntity update = ActorEntityMapper.toEntity(actor);

		// Only update fields that are not null in the input actor
		if (update.getName() != null) {
			actual.setName(update.getName());}
		if (update.getTmdbId() != null) {
			actual.setTmdbId(update.getTmdbId());}

		return ActorEntityMapper.toDomain(actorRepository.save(actual));
	}

	/**
	 * Delete an actor by id.
	 *
	 * @param id actor id
	 * @throws ActorNotFoundException when the actor does not exist
	 */
	public void deleteById(Long id) {
		if (!actorRepository.existsById(id)) {
			throw new ActorNotFoundException("Actor not found: " + id);
		}
		actorRepository.deleteById(id);
	}
}
