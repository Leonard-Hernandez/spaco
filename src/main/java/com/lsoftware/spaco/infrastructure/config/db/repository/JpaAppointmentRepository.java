package com.lsoftware.spaco.infrastructure.config.db.repository;

import com.lsoftware.spaco.infrastructure.config.db.schema.AppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Lookups by id go through findOneById (a JPQL query) so the @TenantId restriction applies;
// em.find() behind findById() does not filter by tenant.
public interface JpaAppointmentRepository extends JpaRepository<AppointmentEntity, Long> {

    Optional<AppointmentEntity> findOneById(Long id);

    Optional<AppointmentEntity> findByManagementToken(UUID token);

    @Query("""
            SELECT a FROM AppointmentEntity a
            WHERE a.professional.id = :professionalId
              AND a.status IN ('PENDING', 'ACCEPTED')
              AND a.startAt < :end
              AND a.endAt > :start
            """)
    List<AppointmentEntity> findActiveOverlapping(@Param("professionalId") Long professionalId,
                                                  @Param("start") OffsetDateTime start,
                                                  @Param("end") OffsetDateTime end);

    @Query("""
            SELECT a FROM AppointmentEntity a
            WHERE a.startAt >= :from
              AND a.startAt < :to
              AND (:status IS NULL OR a.status = :status)
              AND (:professionalId IS NULL OR a.professional.id = :professionalId)
            ORDER BY a.startAt ASC
            """)
    List<AppointmentEntity> findByDateRange(@Param("from") OffsetDateTime from,
                                            @Param("to") OffsetDateTime to,
                                            @Param("status") String status,
                                            @Param("professionalId") Long professionalId);

    List<AppointmentEntity> findByPatientIdOrderByStartAtDesc(Long patientId);

    List<AppointmentEntity> findByStatusOrderByStartAtAsc(String status);
}
