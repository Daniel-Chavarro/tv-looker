package org.tvl.tvlooker.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.exception.ListFavoriteNotFoundException;
import org.tvl.tvlooker.domain.model.dto.ListFavorite;
import org.tvl.tvlooker.domain.model.entity.ItemEntity;
import org.tvl.tvlooker.domain.model.entity.ListFavoriteEntity;
import org.tvl.tvlooker.domain.model.mapper.ItemEntityMapper;
import org.tvl.tvlooker.domain.model.mapper.ListFavoriteEntityMapper;
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

    private static final String LIST_FAVORITE_NOT_FOUND_MESSAGE = "ListFavorite not found: ";

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

    public ListFavorite createForUser(ListFavorite listFavorite, UUID userId) {
        ListFavorite ownedList = ListFavorite.builder()
                .userId(userId)
                .name(listFavorite.getName())
                .description(listFavorite.getDescription())
                .items(listFavorite.getItems())
                .build();
        return create(ownedList);
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
                .orElseThrow(() -> new ListFavoriteNotFoundException(LIST_FAVORITE_NOT_FOUND_MESSAGE + id));
    }

    public ListFavorite getByIdForUser(Long id, UUID userId) {
        return listFavoriteRepository.findByIdAndUserId(id, userId)
                .map(ListFavoriteEntityMapper::toDomain)
                .orElseThrow(() -> new ListFavoriteNotFoundException(LIST_FAVORITE_NOT_FOUND_MESSAGE + id));
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
     * Get all list favorites with pagination.
     *
     * @param pageable pagination information
     * @return page of list favorites
     */
    public Page<ListFavorite> getAll(Pageable pageable) {
        return listFavoriteRepository.findAll(pageable)
                .map(ListFavoriteEntityMapper::toDomain);
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

    public Page<ListFavorite> getByUserId(UUID userId, Pageable pageable) {
        return listFavoriteRepository.findAllByUserId(userId, pageable)
                .map(ListFavoriteEntityMapper::toDomain);
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
            throw new ListFavoriteNotFoundException(LIST_FAVORITE_NOT_FOUND_MESSAGE + id);
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

    public ListFavorite updateForUser(Long id, ListFavorite listFavorite, UUID userId) {
        ListFavoriteEntity actual = listFavoriteRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ListFavoriteNotFoundException(LIST_FAVORITE_NOT_FOUND_MESSAGE + id));

        if (listFavorite.getName() != null) {
            actual.setName(listFavorite.getName());
        }

        if (listFavorite.getDescription() != null) {
            actual.setDescription(listFavorite.getDescription());
        }

        if (listFavorite.getItems() != null) {
            actual.setItems(listFavorite.getItems().stream()
                    .map(ItemEntityMapper::toEntity)
                    .collect(Collectors.toSet()));
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
            throw new ListFavoriteNotFoundException(LIST_FAVORITE_NOT_FOUND_MESSAGE + id);
        }
        listFavoriteRepository.deleteById(id);
    }

    public void deleteByIdForUser(Long id, UUID userId) {
        if (listFavoriteRepository.findByIdAndUserId(id, userId).isEmpty()) {
            throw new ListFavoriteNotFoundException(LIST_FAVORITE_NOT_FOUND_MESSAGE + id);
        }
        listFavoriteRepository.deleteById(id);
    }

    /**
     * Add an item to a favorite list.
     * @param list favorite list
     * @param itemId item to add
     */
    public ListFavorite addItemToFavorite(ListFavorite list, Long itemId) {
        ListFavoriteEntity entity = ListFavoriteEntityMapper.toEntity(list);
        ItemEntity item = ItemEntityMapper.toEntity(itemService.getById(itemId));
        entity.getItems().add(item);
        return ListFavoriteEntityMapper.toDomain(listFavoriteRepository.save(entity));
    }

     /**
     * Remove an item from a favorite list.
     * @param list favorite list
     * @param itemId item to remove
     */
    public ListFavorite removeItemFromFavorite(ListFavorite list, Long itemId) {
        ListFavoriteEntity entity = ListFavoriteEntityMapper.toEntity(list);
        ItemEntity item = ItemEntityMapper.toEntity(itemService.getById(itemId));
        entity.getItems().remove(item);
        return ListFavoriteEntityMapper.toDomain(listFavoriteRepository.save(entity));
    }
}
