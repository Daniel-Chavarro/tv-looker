package org.tvl.tvlooker.domain.model.mapper;

import org.tvl.tvlooker.domain.model.dto.Actor;
import org.tvl.tvlooker.domain.model.dto.ActorItem;
import org.tvl.tvlooker.domain.model.dto.Item;
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
                .itemId(entity.getItem().getId())
                .actorId(entity.getActor().getId())
                .characterName(entity.getCharacterName())
                .billingOrder(entity.getBillingOrder())
                .build();
    }

    /**
     * Converts an ActorItem domain model to an ActorItemEntity.
     * @param domain the domain model
     * @param item the associated Item entity (must be provided to set the relationship)
     * @param actor the associated Actor entity (must be provided to set the relationship)
     * @return the JPA entity
     */
    public static ActorItemEntity toEntity(
            ActorItem domain,
            Item  item,
            Actor actor) {
        return ActorItemEntity.builder()
                .id(domain.getId())
                .actor(ActorEntityMapper.toEntity(actor))
                .item(ItemEntityMapper.toEntity(item))
                .characterName(domain.getCharacterName())
                .billingOrder(domain.getBillingOrder())
                .build();
    }
}
