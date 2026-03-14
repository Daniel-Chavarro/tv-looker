package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tvl.tvlooker.domain.model.entity.Item;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
