package org.tvl.tvlooker.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tvl.tvlooker.api.dto.mapper.DirectorMapper;
import org.tvl.tvlooker.api.dto.response.DirectorResponse;
import org.tvl.tvlooker.domain.model.dto.Director;
import org.tvl.tvlooker.service.DirectorService;

import java.util.List;

/**
 * REST controller for managing directors.
 */
@RestController
@RequestMapping("/api/v1/directors")
@RequiredArgsConstructor
public class DirectorController {
    private final DirectorService directorService;

    /**
     * Retrieves all directors.
     * @return a list of director responses
     */
    @GetMapping
    public ResponseEntity<List<DirectorResponse>> getAllDirectors() {
        List<Director> directors = directorService.getAll();
        return ResponseEntity.ok(directors.stream()
                .map(DirectorMapper::toResponse)
                .toList()
        );
    }

    /**
     * Retrieves a director by its ID.
     * @param id the director ID
     * @return the director response
     */
    @GetMapping("/{id}")
    public ResponseEntity<DirectorResponse> getDirectorById(@PathVariable Long id) {
        Director director = directorService.getById(id);
        return ResponseEntity.ok(DirectorMapper.toResponse(director));
    }
}
