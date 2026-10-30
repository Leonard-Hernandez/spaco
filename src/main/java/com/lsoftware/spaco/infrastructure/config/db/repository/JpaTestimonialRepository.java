package com.lsoftware.spaco.infrastructure.config.db.repository;

import com.lsoftware.spaco.infrastructure.config.db.schema.TestimonialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Lookups by id go through findOneById (a JPQL query) so the @TenantId restriction applies;
// em.find() behind findById() does not filter by tenant.
public interface JpaTestimonialRepository extends JpaRepository<TestimonialEntity, Long> {
    Optional<TestimonialEntity> findOneById(Long id);
    List<TestimonialEntity> findAllByOrderByCreatedAtDesc();
    List<TestimonialEntity> findByStatusOrderByPublishedAtDesc(String status);
    List<TestimonialEntity> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
