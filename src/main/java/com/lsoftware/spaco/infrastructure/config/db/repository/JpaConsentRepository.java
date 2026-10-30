package com.lsoftware.spaco.infrastructure.config.db.repository;

import com.lsoftware.spaco.infrastructure.config.db.schema.ConsentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaConsentRepository extends JpaRepository<ConsentEntity, Long> {
    Optional<ConsentEntity> findOneById(Long id);
    List<ConsentEntity> findByPatientIdOrderByAcceptedAtDesc(Long patientId);
    List<ConsentEntity> findByPatientIdAndTypeAndRevokedAtIsNullOrderByAcceptedAtDesc(Long patientId, String type);
}
