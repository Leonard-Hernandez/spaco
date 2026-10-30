package com.lsoftware.spaco.usecase.consent;

import com.lsoftware.spaco.entity.consent.exception.ConsentTemplateMissingException;
import com.lsoftware.spaco.entity.consent.gateway.ConsentGateway;
import com.lsoftware.spaco.entity.consent.gateway.ConsentTemplateGateway;
import com.lsoftware.spaco.entity.consent.model.Consent;
import com.lsoftware.spaco.entity.consent.model.ConsentChannel;
import com.lsoftware.spaco.entity.consent.model.ConsentTemplate;
import com.lsoftware.spaco.entity.consent.model.ConsentType;
import com.lsoftware.spaco.entity.patient.exception.PatientNotFoundException;
import com.lsoftware.spaco.entity.patient.gateway.PatientGateway;
import com.lsoftware.spaco.usecase.testimonial.TestimonialUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * What each patient authorized, against which text version. Revoking the photo consent takes
 * her testimonials off the website at once.
 */
@Component
@RequiredArgsConstructor
public class ConsentUseCase {

    private final ConsentGateway consents;
    private final ConsentTemplateGateway templates;
    private final PatientGateway patients;
    private final TestimonialUseCase testimonials;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ConsentTemplate currentTemplate(ConsentType type) {
        return templates.findCurrent(type).orElseThrow(ConsentTemplateMissingException::new);
    }

    @Transactional(readOnly = true)
    public List<ConsentTemplate> templates() {
        return templates.findAll();
    }

    /** A new version of the text; consents already given keep the version they accepted. */
    @Transactional
    public ConsentTemplate publishTemplate(ConsentType type, String title, String body) {
        int version = templates.findCurrent(type).map(t -> t.version() + 1).orElse(1);
        return templates.save(new ConsentTemplate(null, type, version, title.strip(), body.strip(), true));
    }

    @Transactional(readOnly = true)
    public List<Consent> list(Long patientId) {
        return consents.findByPatient(patientId);
    }

    @Transactional(readOnly = true)
    public boolean has(Long patientId, ConsentType type) {
        return !consents.findValid(patientId, type).isEmpty();
    }

    /** Idempotent for the general consents: an existing valid one is returned as is. */
    @Transactional
    public Consent grant(Long patientId, ConsentType type, Long treatmentId, ConsentChannel channel, String recordedBy) {
        patients.findById(patientId).orElseThrow(PatientNotFoundException::new);
        if (type != ConsentType.TREATMENT) {
            var existing = consents.findValid(patientId, type);
            if (!existing.isEmpty()) return existing.getFirst();
        }
        ConsentTemplate template = currentTemplate(type);
        return consents.save(new Consent(null, patientId, type, template.version(),
                type == ConsentType.TREATMENT ? treatmentId : null, OffsetDateTime.now(clock), null, channel, recordedBy));
    }

    @Transactional
    public void revoke(Long patientId, ConsentType type) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        consents.findValid(patientId, type).forEach(c -> consents.save(c.revoke(now)));
        if (type == ConsentType.PHOTO_TESTIMONIAL) testimonials.withdrawForPatient(patientId);
    }
}
