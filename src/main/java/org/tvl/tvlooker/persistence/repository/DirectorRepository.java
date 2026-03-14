package org.tvl.tvlooker.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.tvl.tvlooker.domain.model.entity.Director;

interface DirectorRepository extends JpaRepository<Director, Long> {
}
