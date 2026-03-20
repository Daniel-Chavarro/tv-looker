package org.tvl.tvlooker.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.tvl.tvlooker.domain.model.ListFavorite;
import org.tvl.tvlooker.service.ListFavoriteService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lists")
@RequiredArgsConstructor
public class ListFavoriteController {
    private final ListFavoriteService listFavoriteService;

    @GetMapping
    public ResponseEntity<List<ListFavoriteResponse>> listFavorites() {
        List<ListFavorite> favorites = listFavoriteService.getAll();
        return ResponseEntity.ok(favorites.stream()
                .map(ListFavoriteMapper::toResponse)
                .toList());
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<ListFavoriteResponse> getListFavoriteById(@PathVariable Long id) {
        ListFavorite listFavorite = listFavoriteService.getById(id);
        return ResponseEntity.ok(ListFavoriteMapper.toResponse(listFavorite));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ListFavoriteResponse> updateListFavorite(
            @PathVariable Long id,
            @Valid @RequestBody UpdateListFavoriteRequest request) {
        ListFavorite listFavorite = ListFavoriteMapper.fromUpdateRequest(request);
        ListFavorite updated = listFavoriteService.update(id, listFavorite);
        return ResponseEntity.ok(ListFavoriteMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListFavorite(@PathVariable Long id) {
        listFavoriteService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id-list}/add-item/{id-item}")
    public ResponseEntity<ListFavoriteResponse> addItemToFavorite(
            @PathVariable(name = "id-list") Long idList,
            @PathVariable(name = "id-item") Long idItem){
        ListFavorite listFavorite = listFavoriteService.getById(idList);
        ListFavorite updated = listFavoriteService.addItemToFavorite(listFavorite, idItem);
        return ResponseEntity.ok(ListFavoriteMapper.toResponse(updated));
    }

    @PutMapping("/{id-list}/remove-item/{id-item}")
    public ResponseEntity<ListFavoriteResponse> removeItemFromFavorite(
            @PathVariable(name = "id-list") Long idList,
            @PathVariable(name = "id-item") Long idItem){
        ListFavorite listFavorite = listFavoriteService.getById(idList);
        ListFavorite updated = listFavoriteService.removeItemFromFavorite(listFavorite, idItem);
        return ResponseEntity.ok(ListFavoriteMapper.toResponse(updated));
    }
}