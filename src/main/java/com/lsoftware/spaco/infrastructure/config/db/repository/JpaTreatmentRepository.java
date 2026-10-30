package com.lsoftware.spaco.infrastructure.config.db.repository;

import com.lsoftware.spaco.infrastructure.config.db.schema.TreatmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Lookups by id go through findOneById (a JPQL query) so the @TenantId restriction applies;
// em.find() behind findById() does not filter by tenant.
public interface JpaTreatmentRepository extends JpaRepository<TreatmentEntity, Long> {
    Optional<TreatmentEntity> findOneById(Long id);
    Optional<TreatmentEntity> findBySlug(String slug);
    List<TreatmentEntity> findAllByOrderByPositionAscNameAsc();
    boolean existsByCategoryId(Long categoryId);
}
