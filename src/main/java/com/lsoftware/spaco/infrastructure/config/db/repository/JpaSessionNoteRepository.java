package com.lsoftware.spaco.infrastructure.config.db.repository;

import com.lsoftware.spaco.infrastructure.config.db.schema.SessionNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

// Lookups by id go through findOneById (a JPQL query) so the @TenantId restriction applies;
// em.find() behind findById() does not filter by tenant.
public interface JpaSessionNoteRepository extends JpaRepository<SessionNoteEntity, Long> {

    Optional<SessionNoteEntity> findOneById(Long id);

    Optional<SessionNoteEntity> findByAppointmentId(Long appointmentId);

    @Query("""
            SELECT n FROM SessionNoteEntity n
            WHERE n.patientId = :patientId
              AND (cast(:from as timestamp) IS NULL OR n.performedAt >= :from)
              AND (cast(:to as timestamp) IS NULL OR n.performedAt < :to)
            ORDER BY n.performedAt DESC
            """)
    List<SessionNoteEntity> findByPatient(@Param("patientId") Long patientId,
                                          @Param("from") OffsetDateTime from,
                                          @Param("to") OffsetDateTime to);
}
