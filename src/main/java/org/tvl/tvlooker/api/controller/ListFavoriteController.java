package org.tvl.tvlooker.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
            @Valid @RequestBody CreateListFavoriteRequest request) {
        ListFavorite listFavorite = ListFavoriteMapper.fromCreateRequest(request);
        ListFavorite created = listFavoriteService.create(listFavorite);
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
    public ResponseEntity<ListFavoriteResponse> getListFavoriteById(@PathVariable Long id) {
        ListFavorite listFavorite = listFavoriteService.getById(id);
        return ResponseEntity.ok(ListFavoriteMapper.toResponse(listFavorite));
    }

    /**
     * Updates an existing favorite list.
     * @param id the favorite list ID
     * @param request the request containing updated favorite list details
     * @return the updated favorite list response
     */
    @PutMapping("/{id}")
    public ResponseEntity<ListFavoriteResponse> updateListFavorite(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListFavoriteRequest request) {
        ListFavorite listFavorite = ListFavoriteMapper.fromUpdateRequest(request);
        ListFavorite updated = listFavoriteService.update(id, listFavorite);
        return ResponseEntity.ok(ListFavoriteMapper.toResponse(updated));
    }

    /**
     * Deletes a favorite list by its ID.
     * @param id the favorite list ID
     * @return empty response with status 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListFavorite(@PathVariable Long id) {
        listFavoriteService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Adds an item to a favorite list.
     * @param idList the favorite list ID
     * @param idItem the item ID to add
     * @return the updated favorite list response
     */
    @PutMapping("/{id-list}/add-item/{id-item}")
    public ResponseEntity<ListFavoriteResponse> addItemToFavorite(
            @PathVariable(name = "id-list") Long idList,
            @PathVariable(name = "id-item") Long idItem){
        ListFavorite listFavorite = listFavoriteService.getById(idList);
        ListFavorite updated = listFavoriteService.addItemToFavorite(listFavorite, idItem);
        return ResponseEntity.ok(ListFavoriteMapper.toResponse(updated));
    }

    /**
     * Removes an item from a favorite list.
     * @param idList the favorite list ID
     * @param idItem the item ID to remove
     * @return the updated favorite list response
     */
    @PutMapping("/{id-list}/remove-item/{id-item}")
    public ResponseEntity<ListFavoriteResponse> removeItemFromFavorite(
            @PathVariable(name = "id-list") Long idList,
            @PathVariable(name = "id-item") Long idItem){
        ListFavorite listFavorite = listFavoriteService.getById(idList);
        ListFavorite updated = listFavoriteService.removeItemFromFavorite(listFavorite, idItem);
        return ResponseEntity.ok(ListFavoriteMapper.toResponse(updated));
    }
}