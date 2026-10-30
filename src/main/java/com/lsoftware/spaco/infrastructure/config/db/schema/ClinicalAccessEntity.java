package com.lsoftware.spaco.infrastructure.config.db.schema;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.time.OffsetDateTime;

@Entity
@Table(name = "clinical_access_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalAccessEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "actor_sub", nullable = false, length = 64)
    private String actorSub;

    @Column(name = "actor_name", length = 150)
    private String actorName;

    @Column(nullable = false, length = 8)
    private String channel;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(nullable = false)
    private OffsetDateTime at;

    @PrePersist
    void prePersist() {
        if (at == null) at = OffsetDateTime.now();
    }
}
