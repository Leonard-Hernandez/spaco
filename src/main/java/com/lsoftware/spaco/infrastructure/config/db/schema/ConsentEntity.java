package com.lsoftware.spaco.infrastructure.config.db.schema;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.time.OffsetDateTime;

@Entity
@Table(name = "consent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private Long patientId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "template_version", nullable = false)
    private int templateVersion;

    @Column(name = "treatment_id")
    private Long treatmentId;

    @Column(name = "accepted_at", nullable = false)
    private OffsetDateTime acceptedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(nullable = false, length = 10)
    private String channel;

    @Column(name = "recorded_by", length = 120)
    private String recordedBy;
}
