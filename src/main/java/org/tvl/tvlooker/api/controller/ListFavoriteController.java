package org.tvl.tvlooker.api.controller;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.tvl.tvlooker.api.dto.mapper.ListFavoriteMapper;
import org.tvl.tvlooker.api.dto.request.CreateListFavoriteRequest;
import org.tvl.tvlooker.api.dto.request.UpdateListFavoriteRequest;
import org.tvl.tvlooker.api.dto.response.ListFavoriteResponse;
import org.tvl.tvlooker.api.dto.response.PageResponse;
import org.tvl.tvlooker.domain.model.dto.ListFavorite;
import org.tvl.tvlooker.service.ListFavoriteService;

import java.util.UUID;

/**
 * REST controller for managing favorite lists.
 */
@RestController
@RequestMapping("/api/v1/lists")
@RequiredArgsConstructor
public class ListFavoriteController {
    private final ListFavoriteService listFavoriteService;

    /**
     * Retrieves all favorite lists with pagination.
     * @param pageable pagination information
     * @return paginated list of favorite list responses
     */
    @GetMapping
    public ResponseEntity<PageResponse<ListFavoriteResponse>> listFavorites(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ListFavorite> favoritesPage = listFavoriteService.getAll(pageable);
        
        var content = favoritesPage.getContent().stream()
                .map(ListFavoriteMapper::toResponse)
                .toList();
        
        PageResponse<ListFavoriteResponse> response = new PageResponse<>(
                content,
                favoritesPage.getTotalElements(),
                favoritesPage.getNumber(),
                favoritesPage.getTotalPages(),
                favoritesPage.isLast()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Creates a new favorite list.
     * @param request the request containing list favorite details
     * @return the created list favorite response
     */
    @PostMapping
    public ResponseEntity<ListFavoriteResponse> createListFavorite(
            @Valid @RequestBody CreateListFavoriteRequest request,
            Authentication authentication) {
        ListFavorite listFavorite = ListFavoriteMapper.fromCreateRequest(request);
        ListFavorite created = listFavoriteService.createForUser(listFavorite, currentUserId(authentication));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", "/api/v1/users/" + created.getId())
                .body(ListFavoriteMapper.toResponse(created));
    }

    /**
     * Retrieves a favorite list by its ID.
     * @param id the favorite list ID
     * @return the favorite list response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ListFavoriteResponse> getListFavoriteById(
            @PathVariable Long id,
            Authentication authentication) {
        ListFavorite listFavorite = listFavoriteService.getByIdForUser(id, currentUserId(authentication));
        return ResponseEntity.ok(ListFavoriteMapper.toResponse(listFavorite));
    }

    @GetMapping("/me")
    public ResponseEntity<PageResponse<ListFavoriteResponse>> getMyLists(
            Authentication authentication,
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ListFavorite> favoritesPage = listFavoriteService.getByUserId(currentUserId(authentication), pageable);

        var content = favoritesPage.getContent().stream()
                .map(ListFavoriteMapper::toResponse)
                .toList();

        PageResponse<ListFavoriteResponse> response = new PageResponse<>(
                content,
                favoritesPage.getTotalElements(),
                favoritesPage.getNumber(),
                favoritesPage.getTotalPages(),
                favoritesPage.isLast()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponse<ListFavoriteResponse>> getUserLists(
            @PathVariable UUID userId,
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ListFavorite> favoritesPage = listFavoriteService.getByUserId(userId, pageable);
        
        var content = favoritesPage.getContent().stream()
                .map(ListFavoriteMapper::toResponse)
                .toList();
        
        PageResponse<ListFavoriteResponse> response = new PageResponse<>(
                content,
                favoritesPage.getTotalElements(),
                favoritesPage.getNumber(),
                favoritesPage.getTotalPages(),
                favoritesPage.isLast()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing favorite list.
     * @param id the favorite list ID
     * @param request the request containing updated favorite list details
     * @return the updated favorite list response
     */
    @PatchMapping("/{id}")
    public ResponseEntity<ListFavoriteResponse> updateListFavorite(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListFavoriteRequest request,
            Authentication authentication) {
        ListFavorite listFavorite = ListFavoriteMapper.fromUpdateRequest(request);
        ListFavorite updated = listFavoriteService.updateForUser(id, listFavorite, currentUserId(authentication));
        return ResponseEntity.ok(ListFavoriteMapper.toResponse(updated));
    }

    /**
     * Deletes a favorite list by its ID.
     * @param id the favorite list ID
     * @return empty response with status 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListFavorite(@PathVariable Long id, Authentication authentication) {
        listFavoriteService.deleteByIdForUser(id, currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds an item to a favorite list.
     * @param idList the favorite list ID
     * @param idItem the item ID to add
     * @return the updated favorite list response
     */
    @PostMapping("/{id}/items")
    public ResponseEntity<ListFavoriteResponse> addItemToFavorite(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, Long> body,
            Authentication authentication) {
        Long itemId = body.get("itemId");
        ListFavorite listFavorite = listFavoriteService.getByIdForUser(id, currentUserId(authentication));
        ListFavorite updated = listFavoriteService.addItemToFavorite(listFavorite, itemId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ListFavoriteMapper.toResponse(updated));
    }

    /**
     * Removes an item from a favorite list.
     * @param id the favorite list ID
     * @param itemId the item ID to remove
     * @return the updated favorite list response
     */
    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> removeItemFromFavorite(
            @PathVariable Long id,
            @PathVariable Long itemId,
            Authentication authentication) {
        ListFavorite listFavorite = listFavoriteService.getByIdForUser(id, currentUserId(authentication));
        listFavoriteService.removeItemFromFavorite(listFavorite, itemId);
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
