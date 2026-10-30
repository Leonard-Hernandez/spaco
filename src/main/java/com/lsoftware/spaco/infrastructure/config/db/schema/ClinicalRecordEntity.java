package com.lsoftware.spaco.infrastructure.config.db.schema;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.time.OffsetDateTime;

/** One per patient: its id is the patient's. */
@Entity
@Table(name = "clinical_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalRecordEntity {

    @Id
    @Column(name = "patient_id")
    private Long patientId;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(columnDefinition = "text")
    private String allergies;

    @Column(columnDefinition = "text")
    private String medications;

    @Column(columnDefinition = "text")
    private String conditions;

    @Column(length = 120)
    private String pregnancy;

    @Column(name = "skin_type", length = 60)
    private String skinType;

    private Short phototype;

    @Column(columnDefinition = "text")
    private String surgeries;

    @Column(columnDefinition = "text")
    private String habits;

    @Column(columnDefinition = "text")
    private String observations;

    @Column(name = "updated_by", length = 120)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
