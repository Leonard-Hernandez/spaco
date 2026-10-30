package com.lsoftware.spaco.infrastructure.config.db.repository;

import com.lsoftware.spaco.infrastructure.config.db.schema.AvailabilityRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Lookups by id go through findOneById (a JPQL query) so the @TenantId restriction applies;
// em.find() behind findById() does not filter by tenant.
public interface JpaAvailabilityRuleRepository extends JpaRepository<AvailabilityRuleEntity, Long> {
    Optional<AvailabilityRuleEntity> findOneById(Long id);
    List<AvailabilityRuleEntity> findByProfessionalIdAndWeekdayAndActiveTrue(Long professionalId, short weekday);
    List<AvailabilityRuleEntity> findAllByOrderByProfessionalIdAscWeekdayAscStartTimeAsc();
}
