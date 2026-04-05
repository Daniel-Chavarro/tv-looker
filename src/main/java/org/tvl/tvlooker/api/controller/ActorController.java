package org.tvl.tvlooker.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tvl.tvlooker.api.dto.response.ActorResponse;
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
     * @return a list of actor responses
     */
    @GetMapping
    public ResponseEntity<List<ActorResponse>> getAllActors() {
        List<ActorResponse> actors = actorService.getAll().stream()
                .map(actor -> ActorResponse.builder()
                        .id(actor.getId())
                        .name(actor.getName())
                        .tmdbId(actor.getTmdbId())
                        .build())
                .toList();
        return ResponseEntity.ok(actors);
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
