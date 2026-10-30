package com.lsoftware.spaco.usecase.testimonial;

import com.lsoftware.spaco.entity.clinical.exception.PhotoNotFoundException;
import com.lsoftware.spaco.entity.clinical.gateway.SessionNoteGateway;
import com.lsoftware.spaco.entity.clinical.model.SessionPhoto;
import com.lsoftware.spaco.entity.consent.exception.ConsentRequiredException;
import com.lsoftware.spaco.entity.consent.gateway.ConsentGateway;
import com.lsoftware.spaco.entity.consent.model.ConsentType;
import com.lsoftware.spaco.entity.media.gateway.ClinicalPhotoGateway;
import com.lsoftware.spaco.entity.media.gateway.PublicImageGateway;
import com.lsoftware.spaco.entity.patient.exception.PatientNotFoundException;
import com.lsoftware.spaco.entity.patient.gateway.PatientGateway;
import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.entity.testimonial.exception.TestimonialNotFoundException;
import com.lsoftware.spaco.entity.testimonial.gateway.TestimonialGateway;
import com.lsoftware.spaco.entity.testimonial.model.Testimonial;
import com.lsoftware.spaco.entity.testimonial.model.TestimonialPhoto;
import com.lsoftware.spaco.entity.testimonial.model.TestimonialStatus;
import com.lsoftware.spaco.entity.treatment.gateway.TreatmentGateway;
import com.lsoftware.spaco.usecase.testimonial.dto.TestimonialCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Before/after stories for the website. Publishing needs the patient's valid PHOTO_TESTIMONIAL
 * consent and copies the chosen clinical photos to the public store; hiding (or her revoking
 * the consent) deletes those copies, while the clinical originals stay in her record.
 */
@Component
@RequiredArgsConstructor
public class TestimonialUseCase {

    private final TestimonialGateway testimonials;
    private final SessionNoteGateway sessions;
    private final PatientGateway patients;
    private final TreatmentGateway treatments;
    private final ConsentGateway consents;
    private final ClinicalPhotoGateway clinicalPhotos;
    private final PublicImageGateway publicImages;
    private final CurrentTenantGateway tenants;
    private final Clock clock;

    /** Published ones whose patient still consents, for the public carousel. */
    @Transactional(readOnly = true)
    public List<Testimonial> published() {
        return testimonials.findByStatus(TestimonialStatus.PUBLISHED).stream()
                .filter(t -> !consents.findValid(t.patientId(), ConsentType.PHOTO_TESTIMONIAL).isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Testimonial> list() {
        return testimonials.findAll();
    }

    @Transactional(readOnly = true)
    public List<Testimonial> forPatient(Long patientId) {
        return testimonials.findByPatient(patientId);
    }

    @Transactional
    public Testimonial create(TestimonialCommand cmd) {
        patients.findById(cmd.patientId()).orElseThrow(PatientNotFoundException::new);
        String treatmentName = cmd.treatmentId() == null ? null
                : treatments.findById(cmd.treatmentId()).map(t -> t.name()).orElse(null);
        return testimonials.save(new Testimonial(null, cmd.patientId(), cmd.displayName().strip(), cmd.quote(),
                cmd.treatmentId(), treatmentName, TestimonialStatus.DRAFT, null, photos(cmd.patientId(), cmd.photoIds())));
    }

    @Transactional
    public Testimonial update(Long id, TestimonialCommand cmd) {
        Testimonial current = find(id);
        if (current.status() == TestimonialStatus.PUBLISHED) unpublishFiles(current);
        Testimonial edited = current.withText(cmd.displayName().strip(), cmd.quote(), cmd.treatmentId(),
                photos(current.patientId(), cmd.photoIds()));
        Testimonial saved = testimonials.save(edited.with(
                current.status() == TestimonialStatus.PUBLISHED ? TestimonialStatus.DRAFT : current.status(),
                null, edited.photos()));
        return current.status() == TestimonialStatus.PUBLISHED ? publish(saved.id()) : saved;
    }

    @Transactional
    public Testimonial publish(Long id) {
        Testimonial testimonial = find(id);
        if (consents.findValid(testimonial.patientId(), ConsentType.PHOTO_TESTIMONIAL).isEmpty()) {
            throw new ConsentRequiredException("The patient has not authorized publishing her photos");
        }
        String slug = tenants.current().slug();
        List<TestimonialPhoto> published = new ArrayList<>();
        for (TestimonialPhoto photo : testimonial.photos()) {
            String file = photo.publicFile() != null ? photo.publicFile() : copyToPublic(slug, photo.sessionPhotoId());
            published.add(photo.withPublicFile(file));
        }
        return testimonials.save(testimonial.with(TestimonialStatus.PUBLISHED, OffsetDateTime.now(clock), published));
    }

    @Transactional
    public Testimonial hide(Long id) {
        return hide(find(id));
    }

    @Transactional
    public void delete(Long id) {
        unpublishFiles(find(id));
        testimonials.deleteById(id);
    }

    /** Called when the patient revokes her photo consent. */
    @Transactional
    public void withdrawForPatient(Long patientId) {
        testimonials.findByPatient(patientId).stream()
                .filter(t -> t.status() == TestimonialStatus.PUBLISHED)
                .forEach(this::hide);
    }

    private Testimonial hide(Testimonial testimonial) {
        List<TestimonialPhoto> photos = unpublishFiles(testimonial);
        return testimonials.save(testimonial.with(TestimonialStatus.HIDDEN, null, photos));
    }

    private List<TestimonialPhoto> unpublishFiles(Testimonial testimonial) {
        String slug = tenants.current().slug();
        return testimonial.photos().stream().map(p -> {
            if (p.publicFile() != null) publicImages.delete(slug, p.publicFile());
            return p.withPublicFile(null);
        }).toList();
    }

    private String copyToPublic(String slug, Long sessionPhotoId) {
        SessionPhoto photo = sessions.findPhoto(sessionPhotoId).orElseThrow(PhotoNotFoundException::new);
        Path source = clinicalPhotos.find(slug, photo.fileName()).orElseThrow(PhotoNotFoundException::new);
        try {
            String ext = photo.fileName().substring(photo.fileName().lastIndexOf('.') + 1);
            return publicImages.store(slug, Files.readAllBytes(source), ext);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Only photos from this patient's own sessions. */
    private List<TestimonialPhoto> photos(Long patientId, List<Long> photoIds) {
        List<TestimonialPhoto> result = new ArrayList<>();
        for (Long photoId : photoIds == null ? List.<Long>of() : photoIds) {
            SessionPhoto photo = sessions.findPhoto(photoId).orElseThrow(PhotoNotFoundException::new);
            boolean own = sessions.findById(photo.sessionNoteId()).map(n -> n.patientId().equals(patientId)).orElse(false);
            if (!own) throw new PhotoNotFoundException();
            result.add(new TestimonialPhoto(null, photo.id(), photo.kind(), result.size(), null));
        }
        return result;
    }

    private Testimonial find(Long id) {
        return testimonials.findById(id).orElseThrow(TestimonialNotFoundException::new);
    }
}
