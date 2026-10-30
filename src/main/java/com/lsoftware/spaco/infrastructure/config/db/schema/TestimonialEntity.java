package com.lsoftware.spaco.infrastructure.config.db.schema;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "testimonial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestimonialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private Long patientId;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(length = 600)
    private String quote;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "treatment_id")
    private TreatmentEntity treatment;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "testimonial", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    @Builder.Default
    private List<TestimonialPhotoEntity> photos = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
