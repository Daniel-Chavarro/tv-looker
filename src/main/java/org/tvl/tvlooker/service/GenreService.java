package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.exception.GenreNotFoundException;
import org.tvl.tvlooker.domain.model.entity.Genre;
import org.tvl.tvlooker.persistence.repository.GenreRepository;

import java.util.List;

/**
 * Service for Genre entity operations
 */
@Service
@RequiredArgsConstructor
class GenreService {
    private final GenreRepository genreRepository;

    /**
     * Create a new genre.
     *
     * @param genre genre to persist
     * @return saved genre
     */
    public Genre create(Genre genre) {
        return genreRepository.save(genre);
    }

    /**
     * Get a genre by id.
     *
     * @param id genre id
     * @return genre
     * @throws GenreNotFoundException when the genre does not exist
     */
    public Genre getById(Long id) {
        return genreRepository.findById(id)
                .orElseThrow(() -> new GenreNotFoundException("Genre not found: " + id));
    }

    /**
     * Get all genres.
     *
     * @return list of genres
     */
    public List<Genre> getAll() {
        return genreRepository.findAll();
    }

    /**
     * Update a genre.
     *
     * @param id genre id
     * @param genre genre data to update
     * @return updated genre
     * @throws GenreNotFoundException when the genre does not exist
     */
    public Genre update(Long id, Genre genre) {
        if (!genreRepository.existsById(id)) {
            throw new GenreNotFoundException("Genre not found: " + id);
        }
        genre.setId(id);
        return genreRepository.save(genre);
    }

    /**
     * Delete a genre by id.
     *
     * @param id genre id
     * @throws GenreNotFoundException when the genre does not exist
     */
    public void deleteById(Long id) {
        if (!genreRepository.existsById(id)) {
            throw new GenreNotFoundException("Genre not found: " + id);
        }
        genreRepository.deleteById(id);
    }
}
