package org.tvl.tvlooker.domain.model.mapper;

import org.springframework.stereotype.Component;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.domain.model.entity.UserEntity;

/**
 * Mapper for converting between User domain model and UserEntity JPA entity.
 */
@Component
public class UserEntityMapper {

    /**
     * Converts a UserEntity to a User domain model.
     *
     * @param entity the JPA entity
     * @return the domain model
     */
    public static User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return User.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .name(entity.getName())
                .password(entity.getPassword())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Converts a User domain model to a UserEntity JPA entity.
     *
     * @param domain the domain model
     * @return the JPA entity
     */
    public static UserEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }
        return UserEntity.builder()
                .id(domain.getId())
                .username(domain.getUsername())
                .email(domain.getEmail())
                .name(domain.getName())
                .password(domain.getPassword())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
