package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tvl.tvlooker.domain.model.entity.UserEntity;

import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
}
