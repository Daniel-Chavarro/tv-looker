package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.exception.DirectorNotFoundException;
import org.tvl.tvlooker.domain.model.entity.Director;
import org.tvl.tvlooker.persistence.repository.DirectorRepository;

import java.util.List;

/**
 * Service for Director entity operations
 */
@RequiredArgsConstructor
@Service
class DirectorService {

	private final DirectorRepository directorRepository;

	/**
	 * Create a new director.
	 *
	 * @param director director to persist
	 * @return saved director
	 */
	public Director create(Director director) {
		return directorRepository.save(director);
	}

	/**
	 * Get a director by id.
	 *
	 * @param id director id
	 * @return director
	 * @throws DirectorNotFoundException when the director does not exist
	 */
	public Director getById(Long id) {
		return directorRepository.findById(id)
				.orElseThrow(() -> new DirectorNotFoundException("Director not found: " + id));
	}

	/**
	 * Get all directors.
	 *
	 * @return list of directors
	 */
	public List<Director> getAll() {
		return directorRepository.findAll();
	}

	/**
	 * Update a director.
	 *
	 * @param id director id
	 * @param director director data to update
	 * @return updated director
	 * @throws DirectorNotFoundException when the director does not exist
	 */
	public Director update(Long id, Director director) {
		if (!directorRepository.existsById(id)) {
			throw new DirectorNotFoundException("Director not found: " + id);
		}
		director.setId(id);
		return directorRepository.save(director);
	}

	/**
	 * Delete a director by id.
	 *
	 * @param id director id
	 * @throws DirectorNotFoundException when the director does not exist
	 */
	public void deleteById(Long id) {
		if (!directorRepository.existsById(id)) {
			throw new DirectorNotFoundException("Director not found: " + id);
		}
		directorRepository.deleteById(id);
	}

}
