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
import org.tvl.tvlooker.api.dto.response.ActorResponse;
import org.tvl.tvlooker.api.dto.response.PageResponse;
import org.tvl.tvlooker.domain.model.dto.Actor;
import org.tvl.tvlooker.service.ActorService;

import java.util.List;

/**
 * REST controller for managing actors.
 */
@RestController
@RequestMapping("/api/v1/actors")
@RequiredArgsConstructor
public class ActorController {
    private final ActorService actorService;

    /**
     * Retrieves all actors.
     * @param pageable pagination information
     * @return a page of actor responses
     */
    @GetMapping
    public ResponseEntity<PageResponse<ActorResponse>> getAllActors(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Actor> actorsPage = actorService.getAll(pageable);
        
        List<ActorResponse> content = actorsPage.getContent().stream()
                .map(actor -> ActorResponse.builder()
                        .id(actor.getId())
                        .name(actor.getName())
                        .tmdbId(actor.getTmdbId())
                        .build())
                .toList();
        
        PageResponse<ActorResponse> response = new PageResponse<>(
                content,
                actorsPage.getTotalElements(),
                actorsPage.getNumber(),
                actorsPage.getTotalPages(),
                actorsPage.isLast()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves an actor by its ID.
     * @param id the actor ID
     * @return the actor response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ActorResponse> getActorById(@PathVariable Long id) {
        Actor actor = actorService.getById(id);
        ActorResponse response = ActorResponse.builder()
                .id(actor.getId())
                .name(actor.getName())
                .tmdbId(actor.getTmdbId())
                .build();
        return ResponseEntity.ok(response);
    }
}
