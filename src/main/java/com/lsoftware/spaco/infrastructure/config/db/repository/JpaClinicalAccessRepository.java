package com.lsoftware.spaco.infrastructure.config.db.repository;

import com.lsoftware.spaco.infrastructure.config.db.schema.ClinicalAccessEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaClinicalAccessRepository extends JpaRepository<ClinicalAccessEntity, Long> {
    List<ClinicalAccessEntity> findByPatientIdOrderByAtDesc(Long patientId, Limit limit);
}
