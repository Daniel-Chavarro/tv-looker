package org.tvl.tvlooker.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tvl.tvlooker.api.dto.mapper.DirectorMapper;
import org.tvl.tvlooker.api.dto.mapper.PageMapper;
import org.tvl.tvlooker.api.dto.response.DirectorResponse;
import org.tvl.tvlooker.api.dto.response.PageResponse;
import org.tvl.tvlooker.domain.model.dto.Director;
import org.tvl.tvlooker.service.DirectorService;

/**
 * REST controller for managing directors.
 */
@RestController
@RequestMapping("/api/v1/directors")
@RequiredArgsConstructor
public class DirectorController {
    private final DirectorService directorService;

    /**
     * Retrieves all directors with pagination.
     * @param pageable Pagination configuration.
     * @return a paginated list of director responses
     */
    @GetMapping
    public ResponseEntity<PageResponse<DirectorResponse>> getAllDirectors(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Director> directorsPage = directorService.getAll(pageable);
        PageResponse<DirectorResponse> response = PageMapper.toResponse(directorsPage.map(DirectorMapper::toResponse));
        return ResponseEntity.ok(response);
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
