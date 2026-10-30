package com.lsoftware.spaco.usecase.testimonial;

import com.lsoftware.spaco.entity.clinical.exception.PhotoNotFoundException;
import com.lsoftware.spaco.entity.clinical.gateway.SessionNoteGateway;
import com.lsoftware.spaco.entity.clinical.model.PhotoKind;
import com.lsoftware.spaco.entity.clinical.model.SessionNote;
import com.lsoftware.spaco.entity.clinical.model.SessionPhoto;
import com.lsoftware.spaco.entity.consent.exception.ConsentRequiredException;
import com.lsoftware.spaco.entity.consent.gateway.ConsentGateway;
import com.lsoftware.spaco.entity.consent.model.Consent;
import com.lsoftware.spaco.entity.consent.model.ConsentChannel;
import com.lsoftware.spaco.entity.consent.model.ConsentType;
import com.lsoftware.spaco.entity.media.gateway.ClinicalPhotoGateway;
import com.lsoftware.spaco.entity.media.gateway.PublicImageGateway;
import com.lsoftware.spaco.entity.patient.gateway.PatientGateway;
import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.entity.testimonial.gateway.TestimonialGateway;
import com.lsoftware.spaco.entity.testimonial.model.Testimonial;
import com.lsoftware.spaco.entity.testimonial.model.TestimonialPhoto;
import com.lsoftware.spaco.entity.testimonial.model.TestimonialStatus;
import com.lsoftware.spaco.entity.treatment.gateway.TreatmentGateway;
import com.lsoftware.spaco.usecase.testimonial.dto.TestimonialCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.lsoftware.spaco.usecase.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TestimonialUseCaseTest {

    @TempDir
    Path tmp;

    private final TestimonialGateway testimonials = mock(TestimonialGateway.class);
    private final SessionNoteGateway sessions = mock(SessionNoteGateway.class);
    private final PatientGateway patients = mock(PatientGateway.class);
    private final TreatmentGateway treatments = mock(TreatmentGateway.class);
    private final ConsentGateway consents = mock(ConsentGateway.class);
    private final ClinicalPhotoGateway clinical = mock(ClinicalPhotoGateway.class);
    private final PublicImageGateway publicImages = mock(PublicImageGateway.class);
    private final CurrentTenantGateway tenants = mock(CurrentTenantGateway.class);
    private final TestimonialUseCase useCase = new TestimonialUseCase(testimonials, sessions, patients, treatments,
            consents, clinical, publicImages, tenants, CLOCK);

    @BeforeEach
    void setUp() throws IOException {
        when(tenants.current()).thenReturn(SPA);
        when(patients.findById(3L)).thenReturn(Optional.of(patient(3L)));
        when(sessions.findPhoto(70L)).thenReturn(Optional.of(new SessionPhoto(70L, 7L, PhotoKind.BEFORE, "a.jpg", null, null)));
        when(sessions.findById(7L)).thenReturn(Optional.of(note(7L, 3L)));
        when(sessions.findById(8L)).thenReturn(Optional.of(note(8L, 4L)));
        Path photo = Files.write(tmp.resolve("a.jpg"), new byte[]{1, 2, 3});
        when(clinical.find("miel-spa", "a.jpg")).thenReturn(Optional.of(photo));
        when(publicImages.store(eq("miel-spa"), any(), eq("jpg"))).thenReturn("public.jpg");
        when(testimonials.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void onlyPhotosOfThePatientsOwnSessionsCanBeUsed() {
        when(sessions.findPhoto(80L)).thenReturn(Optional.of(new SessionPhoto(80L, 8L, PhotoKind.AFTER, "b.jpg", null, null)));

        assertThatThrownBy(() -> useCase.create(new TestimonialCommand(3L, "Laura, 34", null, null, List.of(70L, 80L))))
                .isInstanceOf(PhotoNotFoundException.class);
    }

    @Test
    void publishingNeedsThePhotoConsent() {
        when(testimonials.findById(1L)).thenReturn(Optional.of(draft()));
        when(consents.findValid(3L, ConsentType.PHOTO_TESTIMONIAL)).thenReturn(List.of());

        assertThatThrownBy(() -> useCase.publish(1L)).isInstanceOf(ConsentRequiredException.class);
        verifyNoInteractions(publicImages);
    }

    @Test
    void publishingCopiesThePhotosToThePublicStore() {
        when(testimonials.findById(1L)).thenReturn(Optional.of(draft()));
        when(consents.findValid(3L, ConsentType.PHOTO_TESTIMONIAL)).thenReturn(List.of(photoConsent()));

        Testimonial published = useCase.publish(1L);

        assertThat(published.status()).isEqualTo(TestimonialStatus.PUBLISHED);
        assertThat(published.photos()).extracting(TestimonialPhoto::publicFile).containsExactly("public.jpg");
        verify(publicImages).store("miel-spa", new byte[]{1, 2, 3}, "jpg");
    }

    @Test
    void withdrawingDeletesThePublicCopiesAndHidesIt() {
        Testimonial live = draft().with(TestimonialStatus.PUBLISHED, OffsetDateTime.now(CLOCK),
                List.of(new TestimonialPhoto(1L, 70L, PhotoKind.BEFORE, 0, "public.jpg")));
        when(testimonials.findByPatient(3L)).thenReturn(List.of(live));

        useCase.withdrawForPatient(3L);

        verify(publicImages).delete("miel-spa", "public.jpg");
        verify(testimonials).save(argThat(t -> t.status() == TestimonialStatus.HIDDEN
                && t.photos().stream().allMatch(p -> p.publicFile() == null)));
    }

    @Test
    void thePublicListSkipsPatientsWhoRevokedLater() {
        Testimonial live = draft().with(TestimonialStatus.PUBLISHED, OffsetDateTime.now(CLOCK), List.of());
        when(testimonials.findByStatus(TestimonialStatus.PUBLISHED)).thenReturn(List.of(live));
        when(consents.findValid(3L, ConsentType.PHOTO_TESTIMONIAL)).thenReturn(List.of());

        assertThat(useCase.published()).isEmpty();
    }

    private static Testimonial draft() {
        return new Testimonial(1L, 3L, "Laura, 34", "Mi piel cambió", null, null, TestimonialStatus.DRAFT, null,
                List.of(new TestimonialPhoto(1L, 70L, PhotoKind.BEFORE, 0, null)));
    }

    private static Consent photoConsent() {
        return new Consent(1L, 3L, ConsentType.PHOTO_TESTIMONIAL, 1, null, OffsetDateTime.now(CLOCK), null,
                ConsentChannel.PORTAL, "Laura");
    }

    private static SessionNote note(long id, long patientId) {
        return new SessionNote(id, patientId, null, null, null, null, null, OffsetDateTime.now(CLOCK), null, null,
                null, null, null, List.of());
    }
}
