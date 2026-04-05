package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.exception.GenreNotFoundException;
import org.tvl.tvlooker.domain.model.Genre;
import org.tvl.tvlooker.domain.model.entity.GenreEntity;
import org.tvl.tvlooker.domain.model.mapper.GenreEntityMapper;
import org.tvl.tvlooker.persistence.repository.GenreRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Genre entity operations
 */
@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;;

    /**
     * Create a new genre.
     *
     * @param genre genre to persist
     * @return saved genre
     */
    public Genre create(Genre genre) {
        GenreEntity entity = GenreEntityMapper.toEntity(genre);
        return GenreEntityMapper.toDomain(genreRepository.save(entity));
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
                .map(GenreEntityMapper::toDomain)
                .orElseThrow(() -> new GenreNotFoundException("Genre not found: " + id));
    }

    /**
     * Get all genres.
     *
     * @return list of genres
     */
    public List<Genre> getAll() {
        return genreRepository.findAll().stream()
                .map(GenreEntityMapper::toDomain)
                .collect(Collectors.toList());
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

        GenreEntity actual = genreRepository.getReferenceById(id);
        GenreEntity update = GenreEntityMapper.toEntity(genre);

        if (update.getName() != null) {
            update.setName(actual.getName());
        }

        if (update.getTmdbId() != null) {
            update.setTmdbId(actual.getTmdbId());
        }

        return GenreEntityMapper.toDomain(genreRepository.save(actual));
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
