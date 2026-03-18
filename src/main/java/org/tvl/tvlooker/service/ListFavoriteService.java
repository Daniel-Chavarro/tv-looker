package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.exception.ListFavoriteNotFoundException;
import org.tvl.tvlooker.domain.model.ListFavorite;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.entity.ListFavoriteEntity;
import org.tvl.tvlooker.persistence.mapper.ItemEntityMapper;
import org.tvl.tvlooker.persistence.mapper.ListFavoriteEntityMapper;
import org.tvl.tvlooker.persistence.repository.ListFavoriteRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for ListFavorite entity operations.
 */
@Service
@RequiredArgsConstructor
public class ListFavoriteService {

    private final ListFavoriteRepository listFavoriteRepository;
    private final ItemService itemService;

    /**
     * Create a new favorite list.
     *
     * @param listFavorite list to persist
     * @return saved list
     */
    public ListFavorite create(ListFavorite listFavorite) {
        ListFavoriteEntity entity = ListFavoriteEntityMapper.toEntity(listFavorite);
        return ListFavoriteEntityMapper.toDomain(listFavoriteRepository.save(entity));
    }

    /**
     * Get a favorite list by id.
     *
     * @param id list id
     * @return favorite list
     * @throws ListFavoriteNotFoundException when the list does not exist
     */
    public ListFavorite getById(Long id) {
        return listFavoriteRepository.findById(id)
                .map(ListFavoriteEntityMapper::toDomain)
                .orElseThrow(() -> new ListFavoriteNotFoundException("ListFavorite not found: " + id));
    }

    /**
     * Get all favorite lists.
     *
     * @return list of favorite lists
     */
    public List<ListFavorite> getAll() {
        return listFavoriteRepository.findAll().stream()
                .map(ListFavoriteEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Get all list favorites for a given user ID.
     *
     * @param userId user id
     * @return favorite lists for the user
     */
    public List<ListFavorite> getListFavorites(UUID userId) {
        return listFavoriteRepository.findByUserId(userId).stream()
                .map(ListFavoriteEntityMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Update a favorite list.
     *
     * @param id list id
     * @param listFavorite list data to update
     * @return updated list
     * @throws ListFavoriteNotFoundException when the list does not exist
     */
    public ListFavorite update(Long id, ListFavorite listFavorite) {
        if (!listFavoriteRepository.existsById(id)) {
            throw new ListFavoriteNotFoundException("ListFavorite not found: " + id);
        }
        ListFavoriteEntity actual = listFavoriteRepository.getReferenceById(id);
        ListFavoriteEntity update = ListFavoriteEntityMapper.toEntity(listFavorite);

        if (update.getName() != null) {
            actual.setName(update.getName());
        }

        if (update.getDescription() != null) {
            actual.setDescription(update.getDescription());
        }

        if (update.getItems() != null) {
            actual.setItems(update.getItems());
        }

        return ListFavoriteEntityMapper.toDomain(listFavoriteRepository.save(actual));
    }

    /**
     * Delete a favorite list by id.
     *
     * @param id list id
     * @throws ListFavoriteNotFoundException when the list does not exist
     */
    public void deleteById(Long id) {
        if (!listFavoriteRepository.existsById(id)) {
            throw new ListFavoriteNotFoundException("ListFavorite not found: " + id);
        }
        listFavoriteRepository.deleteById(id);
    }

    /**
     * Add an item to a favorite list.
     * @param list favorite list
     * @param itemId item to add
     */
    public void addItemToFavorite(ListFavorite list, Long itemId) {
        ListFavoriteEntity entity = ListFavoriteEntityMapper.toEntity(list);
        ItemEntity item = ItemEntityMapper.toEntity(itemService.getById(itemId));
        entity.getItems().add(item);
        listFavoriteRepository.save(entity);
    }

     /**
     * Remove an item from a favorite list.
     * @param list favorite list
     * @param itemId item to remove
     */
    public void removeItemFromFavorite(ListFavorite list, Long itemId) {
        ListFavoriteEntity entity = ListFavoriteEntityMapper.toEntity(list);
        ItemEntity item = ItemEntityMapper.toEntity(itemService.getById(itemId));
        entity.getItems().remove(item);
        listFavoriteRepository.save(entity);}
}
