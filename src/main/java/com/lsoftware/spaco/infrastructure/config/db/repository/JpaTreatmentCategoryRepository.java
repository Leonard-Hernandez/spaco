package com.lsoftware.spaco.infrastructure.config.db.repository;

import com.lsoftware.spaco.infrastructure.config.db.schema.TreatmentCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Lookups by id go through findOneById (a JPQL query) so the @TenantId restriction applies;
// em.find() behind findById() does not filter by tenant.
public interface JpaTreatmentCategoryRepository extends JpaRepository<TreatmentCategoryEntity, Long> {
    Optional<TreatmentCategoryEntity> findOneById(Long id);
    List<TreatmentCategoryEntity> findAllByOrderByPositionAscNameAsc();
    Optional<TreatmentCategoryEntity> findBySlug(String slug);
}
