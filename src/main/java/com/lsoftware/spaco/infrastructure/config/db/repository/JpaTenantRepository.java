package com.lsoftware.spaco.infrastructure.config.db.repository;

import com.lsoftware.spaco.infrastructure.config.db.schema.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaTenantRepository extends JpaRepository<TenantEntity, Long> {
    Optional<TenantEntity> findBySlug(String slug);
}
