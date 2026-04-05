package org.tvl.tvlooker.domain.model.mapper;

import org.tvl.tvlooker.domain.model.dto.ActorItem;
import org.tvl.tvlooker.domain.model.entity.ActorItemEntity;

/**
 * Mapper for converting between ActorItem entities and domain models.
 */
public class ActorItemEntityMapper {

    /**
     * Converts an ActorItemEntity to an ActorItem domain model.
     *
     * @param entity the JPA entity
     * @return the domain model
     */
    public static ActorItem toDomain(ActorItemEntity entity) {
        return ActorItem.builder()
                .id(entity.getId())
                .item(ItemEntityMapper.toDomain(entity.getItem()))
                .actor(ActorEntityMapper.toDomain(entity.getActor()))
                .characterName(entity.getCharacterName())
                .billingOrder(entity.getBillingOrder())
                .build();
    }

    /**
     * Converts an ActorItem domain model to an ActorItemEntity.
     *
     * @param domain the domain model
     * @return the JPA entity
     */
    public static ActorItemEntity toEntity(ActorItem domain) {
        return ActorItemEntity.builder()
                .id(domain.getId())
                .actor(ActorEntityMapper.toEntity(domain.getActor()))
                .item(ItemEntityMapper.toEntity(domain.getItem()))
                .characterName(domain.getCharacterName())
                .billingOrder(domain.getBillingOrder())
                .build();
    }
}
