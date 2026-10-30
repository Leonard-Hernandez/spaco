package com.lsoftware.spaco.infrastructure.config.db.repository;

import com.lsoftware.spaco.infrastructure.config.db.schema.AvailabilityExceptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Lookups by id go through findOneById (a JPQL query) so the @TenantId restriction applies;
// em.find() behind findById() does not filter by tenant.
public interface JpaAvailabilityExceptionRepository extends JpaRepository<AvailabilityExceptionEntity, Long> {

    Optional<AvailabilityExceptionEntity> findOneById(Long id);

    @Query("""
            SELECT e FROM AvailabilityExceptionEntity e
            WHERE e.date = :date
              AND (e.professionalId IS NULL OR e.professionalId = :professionalId)
            """)
    List<AvailabilityExceptionEntity> findForProfessionalOnDate(@Param("professionalId") Long professionalId,
                                                                @Param("date") LocalDate date);

    List<AvailabilityExceptionEntity> findByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);
}
