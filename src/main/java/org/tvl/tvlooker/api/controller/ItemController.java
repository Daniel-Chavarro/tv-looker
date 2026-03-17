package org.tvl.tvlooker.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tvl.tvlooker.api.dto.response.ItemResponse;
import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.service.ItemService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;
    private final ItemMapper itemMapper;

    @GetMapping
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        List<Item> items = itemService.getAll();
        List<ItemResponse> response = items.stream()
                .map(itemMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItemById(@PathVariable Long id) {
        Item item = itemService.getById(id);
        return ResponseEntity.ok(itemMapper.toResponse(item));
    }
}