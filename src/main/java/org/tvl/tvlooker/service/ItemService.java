package org.tvl.tvlooker.service;

import org.tvl.tvlooker.domain.model.dto.Item;
import org.tvl.tvlooker.domain.exception.ItemNotFoundException;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.mapper.ItemEntityMapper;
import org.tvl.tvlooker.persistence.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Consumer;

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


            updateField(update.getTitle(), actual::setTitle);
            updateField(update.getGenres(), actual::setGenres);
            updateField(update.getDirectors(), actual::setDirectors);
            updateField(update.getTmdbId(), actual::setTmdbId);
            updateField(update.getTmdbType(), actual::setTmdbType);
            updateField(update.getOverview(), actual::setOverview);
            updateField(update.getReleaseDate(), actual::setReleaseDate);
            updateField(update.getVoteAverage(), actual::setVoteAverage);
            updateField(update.getPopularity(), actual::setPopularity);

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

    /**
     * Helper method to update a field if the new value is not null.
     *
     * @param value the new value to set
     * @param setter the setter method reference for the field
     * @param <T> the type of the field
     */
    private <T> void updateField(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
