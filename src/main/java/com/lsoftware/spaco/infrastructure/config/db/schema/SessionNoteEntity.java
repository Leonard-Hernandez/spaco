package com.lsoftware.spaco.infrastructure.config.db.schema;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.time.OffsetDateTime;

@Entity
@Table(name = "session_note")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionNoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private Long patientId;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "treatment_id")
    private Long treatmentId;

    @Column(name = "professional_id")
    private Long professionalId;

    @Column(name = "performed_at", nullable = false)
    private OffsetDateTime performedAt;

    @Column(columnDefinition = "text")
    private String products;

    @Column(columnDefinition = "text")
    private String parameters;

    @Column(columnDefinition = "text")
    private String observations;

    @Column(columnDefinition = "text")
    private String recommendations;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
