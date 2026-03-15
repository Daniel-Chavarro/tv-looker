package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.model.entity.Actor;
import org.tvl.tvlooker.domain.exception.ActorNotFoundException;
import org.tvl.tvlooker.persistence.repository.ActorRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
class ActorService {

	private final ActorRepository actorRepository;

	/**
	 * Create a new actor.
	 *
	 * @param actor actor to persist
	 * @return saved actor
	 */
	public Actor create(Actor actor) {
		return actorRepository.save(actor);
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
				.orElseThrow(() -> new ActorNotFoundException("Actor not found: " + id));
	}

	/**
	 * Get all actors.
	 *
	 * @return list of actors
	 */
	public List<Actor> getAll() {
		return actorRepository.findAll();
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
		actor.setId(id);
		return actorRepository.save(actor);
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
