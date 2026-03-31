package org.tvl.tvlooker.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tvl.tvlooker.api.dto.mapper.GenreMapper;
import org.tvl.tvlooker.api.dto.response.GenreResponse;
import org.tvl.tvlooker.domain.model.Genre;
import org.tvl.tvlooker.service.GenreService;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * REST controller for managing genres.
 */
@RestController
@RequestMapping("/api/v1/genres")
@RequiredArgsConstructor
public class GenreController {
    private final GenreService genreService;

    /**
     * Retrieves all genres.
     * @return a list of genre responses
     */
    @GetMapping
    public ResponseEntity<List<GenreResponse>> getAllGenres() {
        List<Genre> genres = genreService.getAll();
        return ResponseEntity.ok(genres.stream()
                .map(GenreMapper::toResponse)
                .collect(toList()));
    }

    /**
     * Retrieves a genre by its ID.
     * @param id the genre ID
     * @return the genre response
     */
    @GetMapping("{id}")
    public ResponseEntity<GenreResponse> getGenreById(@PathVariable Long id) {
        Genre genre = genreService.getById(id);
        return ResponseEntity.ok(GenreMapper.toResponse(genre));
    }
}
