package com.lsoftware.spaco.infrastructure.config.db.schema;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "treatment_id", nullable = false)
    private TreatmentEntity treatment;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "professional_id", nullable = false)
    private ProfessionalEntity professional;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(name = "patient_note", length = 500)
    private String patientNote;

    @Column(name = "management_token", nullable = false, updatable = false)
    private UUID managementToken;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (managementToken == null) managementToken = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (status == null) status = "PENDING";
    }
}
