package com.lsoftware.spaco.infrastructure.config.db.repository;

import com.lsoftware.spaco.infrastructure.config.db.schema.ClinicalRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Lookups by id go through findOneById (a JPQL query) so the @TenantId restriction applies;
// em.find() behind findById() does not filter by tenant.
public interface JpaClinicalRecordRepository extends JpaRepository<ClinicalRecordEntity, Long> {
    Optional<ClinicalRecordEntity> findOneByPatientId(Long patientId);
}
