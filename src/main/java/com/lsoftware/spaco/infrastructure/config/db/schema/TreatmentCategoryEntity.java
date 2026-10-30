package com.lsoftware.spaco.infrastructure.config.db.schema;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "treatment_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 60)
    private String slug;

    @Column(nullable = false, length = 12)
    private String ink;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean active;
}
