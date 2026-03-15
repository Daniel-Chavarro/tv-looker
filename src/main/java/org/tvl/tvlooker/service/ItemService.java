package org.tvl.tvlooker.service;

import org.tvl.tvlooker.domain.model.entity.Item;
import org.tvl.tvlooker.domain.exception.ItemNotFoundException;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Service for Item entity operations.
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    /**
     * Create a new item.
     *
     * @param item item to persist
     * @return saved item
     */
    public Item create(Item item) {
        return itemRepository.save(item);
    }

    /**
     * Get an item by id.
     *
     * @param id item id
     * @return item
     * @throws ItemNotFoundException when the item does not exist
     */
    public Item getById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item not found: " + id));
    }

    /**
     * Get all items.
     *
     * @return list of items
     */
    public List<Item> getAll() {
        return itemRepository.findAll();
    }

    /**
     * Update an item.
     *
     * @param id item id
     * @param item item data to update
     * @return updated item
     * @throws ItemNotFoundException when the item does not exist
     */
    public Item update(Long id, Item item) {
        if (!itemRepository.existsById(id)) {
            throw new ItemNotFoundException("Item not found: " + id);
        }
        item.setId(id);
        return itemRepository.save(item);
    }

    /**
     * Delete an item by id.
     *
     * @param id item id
     * @throws ItemNotFoundException when the item does not exist
     */
    public void deleteById(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new ItemNotFoundException("Item not found: " + id);
        }
        itemRepository.deleteById(id);
    }
}