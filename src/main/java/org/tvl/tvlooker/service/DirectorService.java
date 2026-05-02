package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.exception.DirectorNotFoundException;
import org.tvl.tvlooker.domain.model.dto.Director;
import org.tvl.tvlooker.domain.model.entity.DirectorEntity;
import org.tvl.tvlooker.domain.model.mapper.DirectorEntityMapper;
import org.tvl.tvlooker.persistence.repository.DirectorRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Director entity operations
 */
@RequiredArgsConstructor
@Service
public class DirectorService {

	private final DirectorRepository directorRepository;

	/**
	 * Create a new director.
	 *
	 * @param director director to persist
	 * @return saved director
	 */
	public Director create(Director director) {
		DirectorEntity entity = DirectorEntityMapper.toEntity(director);
		return DirectorEntityMapper.toDomain(directorRepository.save(entity));
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
				.map(DirectorEntityMapper::toDomain)
				.orElseThrow(() -> new DirectorNotFoundException("Director not found: " + id));
	}

	/**
	 * Get all directors.
	 *
	 * @return list of directors
	 */
	public List<Director> getAll() {
		return directorRepository.findAll().stream()
				.map(DirectorEntityMapper::toDomain)
				.collect(Collectors.toList());
	}

	/**
	 * Get all directors with pagination.
	 *
	 * @param pageable pagination information
	 * @return page of directors
	 */
	public Page<Director> getAll(Pageable pageable) {
		return directorRepository.findAll(pageable)
				.map(DirectorEntityMapper::toDomain);
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
		DirectorEntity actual = directorRepository.getReferenceById(id);
		DirectorEntity update = DirectorEntityMapper.toEntity(director);

		if (update.getName() != null) {
			actual.setName(update.getName());
		}
		if (update.getTmdbId() != null) {
			actual.setTmdbId(update.getTmdbId());
		}

		return DirectorEntityMapper.toDomain(directorRepository.save(actual));


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
