package com.lsoftware.spaco.infrastructure.config.db.repository;

import com.lsoftware.spaco.infrastructure.config.db.schema.PatientEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Lookups by id go through findOneById (a JPQL query) so the @TenantId restriction applies;
// em.find() behind findById() does not filter by tenant.
public interface JpaPatientRepository extends JpaRepository<PatientEntity, Long> {

    Optional<PatientEntity> findOneById(Long id);

    Optional<PatientEntity> findByPhoneE164(String phoneE164);

    Optional<PatientEntity> findByAuthcoSub(String authcoSub);

    List<PatientEntity> findByEmailIgnoreCaseAndAuthcoSubIsNull(String email);

    List<PatientEntity> findAllByOrderByCreatedAtDesc(Limit limit);
}
