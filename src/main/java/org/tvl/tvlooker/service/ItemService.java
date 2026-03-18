package org.tvl.tvlooker.service;

import org.tvl.tvlooker.domain.model.Item;
import org.tvl.tvlooker.domain.exception.ItemNotFoundException;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.persistence.mapper.ItemEntityMapper;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

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
        ItemEntity entity = ItemEntityMapper.toEntity(item);
        return ItemEntityMapper.toDomain(itemRepository.save(entity));
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
                .map(ItemEntityMapper::toDomain)
                .orElseThrow(() -> new ItemNotFoundException("Item not found: " + id));
    }

    /**
     * Get all items.
     *
     * @return list of items
     */
    public List<Item> getAll() {
        return itemRepository.findAll().stream()
                .map(ItemEntityMapper::toDomain)
                .collect(Collectors.toList());
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

        ItemEntity actual = itemRepository.getReferenceById(id);
        ItemEntity update = ItemEntityMapper.toEntity(item);

        if (update.getTitle() != null) {
            actual.setTitle(update.getTitle());
        }
        if (update.getGenres() != null) {
            actual.setGenres(update.getGenres());
        }
        if (update.getDirectors() != null) {
            actual.setDirectors(update.getDirectors());
        }
        if (update.getActors() != null) {
            actual.setActors(update.getActors());
        }
        if (update.getTmdbId() != null) {
            actual.setTmdbId(update.getTmdbId());
        }
        if (update.getTmdbType() != null) {
            actual.setTmdbType(update.getTmdbType());
        }
        if (update.getOverview() != null) {
            actual.setOverview(update.getOverview());
        }
        if (update.getReleaseDate() != null) {
            actual.setReleaseDate(update.getReleaseDate());
        }
        if (update.getVoteAverage() != null) {
            actual.setVoteAverage(update.getVoteAverage());
        }
        if (update.getPopularity() != null) {
            actual.setPopularity(update.getPopularity());
        }

        return ItemEntityMapper.toDomain(itemRepository.save(actual));
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
