package org.tvl.tvlooker.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tvl.tvlooker.api.dto.mapper.InteractionMapper;
import org.tvl.tvlooker.api.dto.request.CreateInteractionRequest;
import org.tvl.tvlooker.api.dto.request.UpdateInteractionRequest;
import org.tvl.tvlooker.api.dto.response.InteractionResponse;
import org.tvl.tvlooker.api.dto.response.PageResponse;
import org.tvl.tvlooker.domain.model.dto.Interaction;
import org.tvl.tvlooker.service.InteractionService;


import java.util.List;
import java.util.UUID;

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
    public ResponseEntity<PageResponse<InteractionResponse>> getAllInteractions(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Interaction> interactionsPage = interactionService.getAll(pageable);

        List<InteractionResponse> content = interactionsPage.getContent().stream()
                .map(InteractionMapper::toResponse)
                .toList();

        PageResponse<InteractionResponse> response = new PageResponse<>(
                content,
                interactionsPage.getTotalElements(),
                interactionsPage.getNumber(),
                interactionsPage.getTotalPages(),
                interactionsPage.isLast()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new interaction.
     * @param request the request body containing the details of the interaction to create
     * @return the created interaction response with a 201 Created status, or a 400 Bad Request status if the request is
     *         invalid
     */
    @PostMapping
    public ResponseEntity<InteractionResponse> createInteraction(
            @RequestBody CreateInteractionRequest request,
            Authentication authentication) {
        Interaction interaction = InteractionMapper.fromCreateRequest(request);
        Interaction created = interactionService.createForUser(interaction, currentUserId(authentication));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/v1/users/" + created.getId())
                .body(InteractionMapper.toResponse(created));
    }

    @GetMapping("/me")
    public ResponseEntity<PageResponse<InteractionResponse>> getMyInteractions(
            Authentication authentication,
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Interaction> interactionsPage = interactionService.getByUserId(currentUserId(authentication), pageable);

        List<InteractionResponse> content = interactionsPage.getContent().stream()
                .map(InteractionMapper::toResponse)
                .toList();

        PageResponse<InteractionResponse> response = new PageResponse<>(
                content,
                interactionsPage.getTotalElements(),
                interactionsPage.getNumber(),
                interactionsPage.getTotalPages(),
                interactionsPage.isLast()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves an interaction by its ID.
     * @param id the interaction ID
     * @return the interaction response
     */
    @GetMapping("/{id}")
    public ResponseEntity<InteractionResponse> getInteractionById(
            @PathVariable Long id,
            Authentication authentication) {
        Interaction interaction = interactionService.getByIdForUser(id, currentUserId(authentication));
        return ResponseEntity.ok(InteractionMapper.toResponse(interaction));
    }


    /**
     * Updates an existing interaction.
     * @param id the interaction ID
     * @param request the request containing updated interaction details
     * @return the updated interaction response
     */
    @PatchMapping("/{id}")
    public ResponseEntity<InteractionResponse> updateInteraction(
            @PathVariable Long id,
            @RequestBody UpdateInteractionRequest request,
            Authentication authentication) {
        Interaction interaction = InteractionMapper.fromUpdateRequest(request);
        Interaction updated = interactionService.updateForUser(id, interaction, currentUserId(authentication));
        return ResponseEntity.ok(InteractionMapper.toResponse(updated));
    }

    /**
     * Deletes an interaction by its ID.
     * @param id the interaction ID
     * @return empty response with status 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInteraction(@PathVariable Long id, Authentication authentication) {
        interactionService.deleteForUser(id, currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
