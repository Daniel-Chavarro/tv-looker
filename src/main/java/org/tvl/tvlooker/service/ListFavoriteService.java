package org.tvl.tvlooker.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tvl.tvlooker.domain.model.entity.ListFavorite;
import org.tvl.tvlooker.persistence.repository.ListFavoriteRepository;

import java.util.List;
import java.util.UUID;

/**
 * Service for ListFavorite entity operations.
 */
@Service
@RequiredArgsConstructor
class ListFavoriteService {

    private final ListFavoriteRepository reviewRepository;

    /**
     * Create a new favorite list.
     *
     * @param listFavorite list to persist
     * @return saved list
     */
    public ListFavorite create(ListFavorite listFavorite) {
        return reviewRepository.save(listFavorite);
    }

    /**
     * Get a favorite list by id.
     *
     * @param id list id
     * @return favorite list
     * @throws EntityNotFoundException when the list does not exist
     */
    public ListFavorite getById(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ListFavorite not found: " + id));
    }

    /**
     * Get all favorite lists.
     *
     * @return list of favorite lists
     */
    public List<ListFavorite> getAll() {
        return reviewRepository.findAll();
    }

    /**
     * Get all list favorites for a given user ID.
     *
     * @param userId user id
     * @return favorite lists for the user
     */
    public List<ListFavorite> getListFavorites(UUID userId) {
        return reviewRepository.findByUserId(userId);
    }

    /**
     * Update a favorite list.
     *
     * @param id list id
     * @param listFavorite list data to update
     * @return updated list
     * @throws EntityNotFoundException when the list does not exist
     */
    public ListFavorite update(Long id, ListFavorite listFavorite) {
        if (!reviewRepository.existsById(id)) {
            throw new EntityNotFoundException("ListFavorite not found: " + id);
        }
        listFavorite.setId(id);
        return reviewRepository.save(listFavorite);
    }

    /**
     * Delete a favorite list by id.
     *
     * @param id list id
     * @throws EntityNotFoundException when the list does not exist
     */
    public void deleteById(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new EntityNotFoundException("ListFavorite not found: " + id);
        }
        reviewRepository.deleteById(id);
    }
}
