package org.tvl.tvlooker.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tvl.tvlooker.api.dto.mapper.ItemMapper;
import org.tvl.tvlooker.api.dto.response.ItemResponse;
import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.service.ItemService;

import java.util.List;

/**
 * REST controller for managing items.
 */
@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {
    /**
     * Service for managing items.
     */
    private final ItemService ITEM_SERVICE;
    
    /**
     * Retrieves all items.
     * @return A list of item responses.
     */
    @GetMapping
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        List<Item> items = ITEM_SERVICE.getAll();
        List<ItemResponse> response = items.stream()
                .map(ItemMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves an item by its ID.
     * @param id The ID of the item to retrieve.
     * @return The item response if found, or a 404 Not Found status if the item does not exist.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItemById(@PathVariable Long id) {
        Item item = ITEM_SERVICE.getById(id);
        return ResponseEntity.ok(ItemMapper.toResponse(item));
    }
}