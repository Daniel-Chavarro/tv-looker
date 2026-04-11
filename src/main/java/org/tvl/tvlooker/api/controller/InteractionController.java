package org.tvl.tvlooker.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tvl.tvlooker.api.dto.mapper.InteractionMapper;
import org.tvl.tvlooker.api.dto.request.CreateInteractionRequest;
import org.tvl.tvlooker.api.dto.request.UpdateInteractionRequest;
import org.tvl.tvlooker.api.dto.response.InteractionResponse;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.service.InteractionService;


import java.util.List;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
public class InteractionController {
    /**
     * Service for managing interactions.
     */
    private final InteractionService interactionService;

    /**
     * Retrieves all interactions.
     * @return list of interaction responses
     */
    @GetMapping
    public ResponseEntity<List<InteractionResponse>> getAllInteractions(){
        List<Interaction> interactions = interactionService.getAll();;
        List<InteractionResponse> response = interactions.stream()
                .map(InteractionMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new interaction.
     * @param request the request body containing the details of the interaction to create
     * @return the created interaction response with a 201 Created status, or a 400 Bad Request status if the request is
     *         invalid
     */
    @PostMapping
    public ResponseEntity<InteractionResponse> createInteraction(@RequestBody CreateInteractionRequest request) {
        Interaction interaction = InteractionMapper.fromCreateRequest(request);
        Interaction created = interactionService.create(interaction);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/v1/users/" + created.getId())
                .body(InteractionMapper.toResponse(created));
    }

    /**
     * Retrieves an interaction by its ID.
     * @param id the interaction ID
     * @return the interaction response
     */
    @GetMapping("/{id}")
    public ResponseEntity<InteractionResponse> getInteractionById(@PathVariable Long id) {
        Interaction interaction = interactionService.getById(id);
        return ResponseEntity.ok(InteractionMapper.toResponse(interaction));
    }


    /**
     * Updates an existing interaction.
     * @param id the interaction ID
     * @param request the request containing updated interaction details
     * @return the updated interaction response
     */
    @PutMapping("/{id}")
    public ResponseEntity<InteractionResponse> updateInteraction(
            @PathVariable Long id,
            @RequestBody UpdateInteractionRequest request) {
        Interaction interaction = InteractionMapper.fromUpdateRequest(request);
        Interaction updated = interactionService.update(id, interaction);
        return ResponseEntity.ok(InteractionMapper.toResponse(updated));
    }

    /**
     * Deletes an interaction by its ID.
     * @param id the interaction ID
     * @return empty response with status 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInteraction(@PathVariable Long id) {
        interactionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
