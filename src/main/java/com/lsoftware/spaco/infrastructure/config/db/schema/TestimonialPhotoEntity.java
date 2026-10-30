package com.lsoftware.spaco.infrastructure.config.db.schema;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

@Entity
@Table(name = "testimonial_photo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestimonialPhotoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "testimonial_id", nullable = false)
    private TestimonialEntity testimonial;

    @Column(name = "session_photo_id", nullable = false)
    private Long sessionPhotoId;

    @Column(nullable = false, length = 8)
    private String kind;

    @Column(nullable = false)
    private int position;

    @Column(name = "public_file", length = 80)
    private String publicFile;
}
