package com.lsoftware.spaco.usecase.consent;

import com.lsoftware.spaco.entity.consent.gateway.ConsentGateway;
import com.lsoftware.spaco.entity.consent.gateway.ConsentTemplateGateway;
import com.lsoftware.spaco.entity.consent.model.Consent;
import com.lsoftware.spaco.entity.consent.model.ConsentChannel;
import com.lsoftware.spaco.entity.consent.model.ConsentTemplate;
import com.lsoftware.spaco.entity.consent.model.ConsentType;
import com.lsoftware.spaco.entity.patient.gateway.PatientGateway;
import com.lsoftware.spaco.usecase.testimonial.TestimonialUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.lsoftware.spaco.usecase.Fixtures.CLOCK;
import static com.lsoftware.spaco.usecase.Fixtures.patient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConsentUseCaseTest {

    private final ConsentGateway consents = mock(ConsentGateway.class);
    private final ConsentTemplateGateway templates = mock(ConsentTemplateGateway.class);
    private final PatientGateway patients = mock(PatientGateway.class);
    private final TestimonialUseCase testimonials = mock(TestimonialUseCase.class);
    private final ConsentUseCase useCase = new ConsentUseCase(consents, templates, patients, testimonials, CLOCK);

    @BeforeEach
    void setUp() {
        when(patients.findById(3L)).thenReturn(Optional.of(patient(3L)));
        when(templates.findCurrent(ConsentType.PHOTO_TESTIMONIAL)).thenReturn(Optional.of(
                new ConsentTemplate(1L, ConsentType.PHOTO_TESTIMONIAL, 2, "Fotos", "Texto", true)));
        when(consents.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void aConsentRecordsTheVersionOfTheTextAccepted() {
        when(consents.findValid(3L, ConsentType.PHOTO_TESTIMONIAL)).thenReturn(List.of());

        Consent given = useCase.grant(3L, ConsentType.PHOTO_TESTIMONIAL, null, ConsentChannel.PORTAL, "Laura");

        assertThat(given.templateVersion()).isEqualTo(2);
        assertThat(given.acceptedAt()).isEqualTo(OffsetDateTime.now(CLOCK));
    }

    @Test
    void revokingThePhotoConsentWithdrawsTheTestimonials() {
        Consent valid = new Consent(1L, 3L, ConsentType.PHOTO_TESTIMONIAL, 1, null, OffsetDateTime.now(CLOCK), null,
                ConsentChannel.PORTAL, "Laura");
        when(consents.findValid(3L, ConsentType.PHOTO_TESTIMONIAL)).thenReturn(List.of(valid));

        useCase.revoke(3L, ConsentType.PHOTO_TESTIMONIAL);

        verify(consents).save(argThat(c -> c.revokedAt() != null));
        verify(testimonials).withdrawForPatient(3L);
    }
}
