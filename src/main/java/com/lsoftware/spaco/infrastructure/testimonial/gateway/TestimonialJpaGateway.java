package com.lsoftware.spaco.infrastructure.testimonial.gateway;

import com.lsoftware.spaco.entity.clinical.model.PhotoKind;
import com.lsoftware.spaco.entity.testimonial.gateway.TestimonialGateway;
import com.lsoftware.spaco.entity.testimonial.model.Testimonial;
import com.lsoftware.spaco.entity.testimonial.model.TestimonialPhoto;
import com.lsoftware.spaco.entity.testimonial.model.TestimonialStatus;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaTestimonialRepository;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaTreatmentRepository;
import com.lsoftware.spaco.infrastructure.config.db.schema.TestimonialEntity;
import com.lsoftware.spaco.infrastructure.config.db.schema.TestimonialPhotoEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TestimonialJpaGateway implements TestimonialGateway {

    private final JpaTestimonialRepository repo;
    private final JpaTreatmentRepository treatments;

    @Override
    public Testimonial save(Testimonial t) {
        TestimonialEntity entity = t.id() != null ? repo.findOneById(t.id()).orElseThrow()
                : TestimonialEntity.builder().patientId(t.patientId()).build();
        entity.setDisplayName(t.displayName());
        entity.setQuote(t.quote());
        entity.setTreatment(t.treatmentId() == null ? null : treatments.findOneById(t.treatmentId()).orElse(null));
        entity.setStatus(t.status().name());
        entity.setPublishedAt(t.publishedAt());
        // Replace the photo rows (orphan removal deletes the old ones)
        entity.getPhotos().clear();
        for (TestimonialPhoto p : t.photos()) {
            entity.getPhotos().add(TestimonialPhotoEntity.builder()
                    .testimonial(entity).sessionPhotoId(p.sessionPhotoId()).kind(p.kind().name())
                    .position(p.position()).publicFile(p.publicFile())
                    .build());
        }
        return toDomain(repo.saveAndFlush(entity));
    }

    @Override
    public Optional<Testimonial> findById(Long id) {
        return repo.findOneById(id).map(this::toDomain);
    }

    @Override
    public List<Testimonial> findAll() {
        return repo.findAllByOrderByCreatedAtDesc().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Testimonial> findByStatus(TestimonialStatus status) {
        return repo.findByStatusOrderByPublishedAtDesc(status.name()).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Testimonial> findByPatient(Long patientId) {
        return repo.findByPatientIdOrderByCreatedAtDesc(patientId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        repo.findOneById(id).ifPresent(repo::delete);
    }

    private Testimonial toDomain(TestimonialEntity e) {
        return new Testimonial(e.getId(), e.getPatientId(), e.getDisplayName(), e.getQuote(),
                e.getTreatment() == null ? null : e.getTreatment().getId(),
                e.getTreatment() == null ? null : e.getTreatment().getName(),
                TestimonialStatus.valueOf(e.getStatus()), e.getPublishedAt(),
                e.getPhotos().stream().map(p -> new TestimonialPhoto(p.getId(), p.getSessionPhotoId(),
                        PhotoKind.valueOf(p.getKind()), p.getPosition(), p.getPublicFile())).toList());
    }
}
