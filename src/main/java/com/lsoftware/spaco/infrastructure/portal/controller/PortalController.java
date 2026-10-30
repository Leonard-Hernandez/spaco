package com.lsoftware.spaco.infrastructure.portal.controller;

import com.lsoftware.spaco.entity.clinical.model.AccessChannel;
import com.lsoftware.spaco.entity.clinical.model.Actor;
import com.lsoftware.spaco.entity.clinical.model.ClinicalAccess;
import com.lsoftware.spaco.entity.clinical.model.ClinicalRecord;
import com.lsoftware.spaco.entity.clinical.model.SessionNote;
import com.lsoftware.spaco.entity.consent.model.Consent;
import com.lsoftware.spaco.entity.consent.model.ConsentChannel;
import com.lsoftware.spaco.entity.consent.model.ConsentType;
import com.lsoftware.spaco.entity.exception.DomainException;
import com.lsoftware.spaco.entity.patient.model.Patient;
import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.infrastructure.appointment.dto.BookingResponse;
import com.lsoftware.spaco.infrastructure.clinical.controller.PhotoResponses;
import com.lsoftware.spaco.infrastructure.patient.dto.PatientResponse;
import com.lsoftware.spaco.infrastructure.testimonial.dto.PublicTestimonialResponse;
import com.lsoftware.spaco.usecase.appointment.ManageAppointmentUseCase;
import com.lsoftware.spaco.usecase.clinical.ClinicalRecordUseCase;
import com.lsoftware.spaco.usecase.clinical.SessionNoteUseCase;
import com.lsoftware.spaco.usecase.consent.ConsentUseCase;
import com.lsoftware.spaco.usecase.patient.ResolvePatientUseCase;
import com.lsoftware.spaco.usecase.testimonial.TestimonialUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The patient's own history (signed in with Google). The first call links her account to the
 * patient registered with the same verified email.
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "My history (patient)", description = "What the spa recorded about me")
@SecurityRequirement(name = "bearerAuth")
public class PortalController {

    private final ResolvePatientUseCase resolvePatient;
    private final ClinicalRecordUseCase records;
    private final SessionNoteUseCase sessions;
    private final ManageAppointmentUseCase appointments;
    private final ConsentUseCase consents;
    private final TestimonialUseCase testimonials;
    private final CurrentTenantGateway tenants;

    @GetMapping
    @Operation(summary = "My patient profile (404 PATIENT_NOT_LINKED if no patient uses my email)")
    public PatientResponse me(@AuthenticationPrincipal Jwt jwt) {
        return PatientResponse.from(patient(jwt));
    }

    @GetMapping("/record")
    public ClinicalRecord record(@AuthenticationPrincipal Jwt jwt) {
        Patient p = patient(jwt);
        return records.get(p.id(), actor(jwt, p));
    }

    @GetMapping("/sessions")
    public List<SessionNote> sessions(@AuthenticationPrincipal Jwt jwt) {
        Patient p = patient(jwt);
        return sessions.list(p.id(), null, null, actor(jwt, p));
    }

    @GetMapping("/photos/{photoId}")
    public ResponseEntity<Resource> photo(@PathVariable Long photoId, @AuthenticationPrincipal Jwt jwt) {
        return PhotoResponses.of(sessions.photo(photoId, patient(jwt).id()));
    }

    @GetMapping("/appointments")
    public List<BookingResponse> appointments(@AuthenticationPrincipal Jwt jwt) {
        return appointments.ofPatient(patient(jwt).id()).stream().map(BookingResponse::from).toList();
    }

    @GetMapping("/consents")
    public List<Consent> consents(@AuthenticationPrincipal Jwt jwt) {
        return consents.list(patient(jwt).id());
    }

    /** Only the photo consent is given from here; the others are given when booking or at the spa. */
    @PostMapping("/consents/{type}")
    @ResponseStatus(HttpStatus.CREATED)
    public Consent grant(@PathVariable ConsentType type, @AuthenticationPrincipal Jwt jwt) {
        if (type != ConsentType.PHOTO_TESTIMONIAL) {
            throw new DomainException("VALIDATION_ERROR", "Only the photo consent can be given here");
        }
        Patient p = patient(jwt);
        return consents.grant(p.id(), type, null, ConsentChannel.PORTAL, p.fullName());
    }

    @DeleteMapping("/consents/{type}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a consent; revoking the photo one takes my testimonials off the site")
    public void revoke(@PathVariable ConsentType type, @AuthenticationPrincipal Jwt jwt) {
        consents.revoke(patient(jwt).id(), type);
    }

    @GetMapping("/testimonials")
    public List<PublicTestimonialResponse> testimonials(@AuthenticationPrincipal Jwt jwt) {
        String slug = tenants.current().slug();
        return testimonials.forPatient(patient(jwt).id()).stream()
                .map(t -> PublicTestimonialResponse.from(t, slug)).toList();
    }

    @GetMapping("/access-log")
    @Operation(summary = "Who looked at or changed my history")
    public List<ClinicalAccess> accessLog(@AuthenticationPrincipal Jwt jwt) {
        return records.accessLog(patient(jwt).id());
    }

    private Patient patient(Jwt jwt) {
        return resolvePatient.execute(jwt.getSubject(), jwt.getTokenValue());
    }

    private static Actor actor(Jwt jwt, Patient p) {
        return new Actor(jwt.getSubject(), p.fullName(), AccessChannel.PORTAL);
    }
}
