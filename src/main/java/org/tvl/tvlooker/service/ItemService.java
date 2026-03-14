package org.tvl.tvlooker.service;

import org.tvl.tvlooker.domain.model.entity.Item;
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
     * Get all items in the system.
     * Used by recommendation engine as candidate items and for similarity computation.
     *
     * @return list of all items
     */
    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }
}