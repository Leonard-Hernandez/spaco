package com.lsoftware.spaco.infrastructure.clinical.controller;

import com.lsoftware.spaco.entity.clinical.model.AccessChannel;
import com.lsoftware.spaco.entity.clinical.model.Actor;
import com.lsoftware.spaco.entity.clinical.model.ClinicalAccess;
import com.lsoftware.spaco.entity.clinical.model.ClinicalRecord;
import com.lsoftware.spaco.entity.clinical.model.NoteMatch;
import com.lsoftware.spaco.entity.clinical.model.PhotoKind;
import com.lsoftware.spaco.entity.clinical.model.SessionNote;
import com.lsoftware.spaco.entity.clinical.model.SessionPhoto;
import com.lsoftware.spaco.entity.consent.model.Consent;
import com.lsoftware.spaco.entity.consent.model.ConsentChannel;
import com.lsoftware.spaco.entity.consent.model.ConsentType;
import com.lsoftware.spaco.entity.member.model.TenantMember;
import com.lsoftware.spaco.infrastructure.clinical.dto.ConsentRequest;
import com.lsoftware.spaco.infrastructure.clinical.dto.RecordRequest;
import com.lsoftware.spaco.infrastructure.clinical.dto.SessionNoteRequest;
import com.lsoftware.spaco.infrastructure.config.security.Actors;
import com.lsoftware.spaco.infrastructure.config.security.PanelAuthorizationManager;
import com.lsoftware.spaco.usecase.clinical.ClinicalRecordUseCase;
import com.lsoftware.spaco.usecase.clinical.PatientSummaryUseCase;
import com.lsoftware.spaco.usecase.clinical.SessionNoteUseCase;
import com.lsoftware.spaco.usecase.clinical.dto.ContraindicationCheck;
import com.lsoftware.spaco.usecase.clinical.dto.PatientSummary;
import com.lsoftware.spaco.usecase.consent.ConsentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

/** Clinical history, for the owner and estheticians. Every call is in the access log. */
@RestController
@RequestMapping("/api/panel/clinical")
@RequiredArgsConstructor
@Tag(name = "Clinical record (esthetician)", description = "Anamnesis, sessions, photos, consents")
@SecurityRequirement(name = "bearerAuth")
public class ClinicalController {

    private final ClinicalRecordUseCase records;
    private final SessionNoteUseCase sessions;
    private final PatientSummaryUseCase summaries;
    private final ConsentUseCase consents;

    @GetMapping("/patients/{id}/summary")
    @Operation(summary = "Anamnesis, alerts, last sessions and upcoming appointments")
    public PatientSummary summary(@PathVariable Long id, @RequestAttribute(PanelAuthorizationManager.MEMBER_ATTRIBUTE) TenantMember m) {
        return summaries.summary(id, actor(m));
    }

    @GetMapping("/patients/{id}/record")
    public ClinicalRecord record(@PathVariable Long id, @RequestAttribute(PanelAuthorizationManager.MEMBER_ATTRIBUTE) TenantMember m) {
        return records.get(id, actor(m));
    }

    @PutMapping("/patients/{id}/record")
    public ClinicalRecord updateRecord(@PathVariable Long id, @Valid @RequestBody RecordRequest req,
                                       @RequestAttribute(PanelAuthorizationManager.MEMBER_ATTRIBUTE) TenantMember m) {
        return records.update(id, req.toCommand(), actor(m));
    }

    @GetMapping("/patients/{id}/sessions")
    public List<SessionNote> sessions(@PathVariable Long id,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
                                      @RequestAttribute(PanelAuthorizationManager.MEMBER_ATTRIBUTE) TenantMember m) {
        return sessions.list(id, from, to, actor(m));
    }

    @PostMapping("/patients/{id}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Note of a visit; with appointmentId it also completes that appointment")
    public SessionNote createSession(@PathVariable Long id, @Valid @RequestBody SessionNoteRequest req,
                                     @RequestAttribute(PanelAuthorizationManager.MEMBER_ATTRIBUTE) TenantMember m) {
        return sessions.create(id, req.toCommand(), actor(m));
    }

    @GetMapping("/sessions/{noteId}")
    public SessionNote session(@PathVariable Long noteId, @RequestAttribute(PanelAuthorizationManager.MEMBER_ATTRIBUTE) TenantMember m) {
        return sessions.get(noteId, actor(m));
    }

    @PutMapping("/sessions/{noteId}")
    public SessionNote updateSession(@PathVariable Long noteId, @Valid @RequestBody SessionNoteRequest req,
                                     @RequestAttribute(PanelAuthorizationManager.MEMBER_ATTRIBUTE) TenantMember m) {
        return sessions.update(noteId, req.toCommand(), actor(m));
    }

    @PostMapping(value = "/sessions/{noteId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Before/after photo (location metadata is stripped)")
    public SessionPhoto addPhoto(@PathVariable Long noteId, @RequestParam("file") MultipartFile file,
                                 @RequestParam PhotoKind kind, @RequestParam(required = false) String caption,
                                 @RequestAttribute(PanelAuthorizationManager.MEMBER_ATTRIBUTE) TenantMember m) throws IOException {
        return sessions.addPhoto(noteId, kind, file.getBytes(), caption, actor(m));
    }

    @GetMapping("/photos/{photoId}")
    public ResponseEntity<Resource> photo(@PathVariable Long photoId) {
        return PhotoResponses.of(sessions.photo(photoId, null));
    }

    @DeleteMapping("/photos/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@PathVariable Long photoId, @RequestAttribute(PanelAuthorizationManager.MEMBER_ATTRIBUTE) TenantMember m) {
        sessions.deletePhoto(photoId, actor(m));
    }

    @GetMapping("/patients/{id}/contraindications")
    @Operation(summary = "Treatment contraindications next to the patient's history")
    public ContraindicationCheck contraindications(@PathVariable Long id, @RequestParam Long treatmentId,
                                                   @RequestAttribute(PanelAuthorizationManager.MEMBER_ATTRIBUTE) TenantMember m) {
        return summaries.checkContraindications(id, treatmentId, actor(m));
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search over session notes (one patient or all)")
    public List<NoteMatch> search(@RequestParam String q, @RequestParam(required = false) Long patientId,
                                  @RequestAttribute(PanelAuthorizationManager.MEMBER_ATTRIBUTE) TenantMember m) {
        return summaries.searchNotes(q, patientId, actor(m));
    }

    @GetMapping("/patients/{id}/consents")
    public List<Consent> consents(@PathVariable Long id) {
        return consents.list(id);
    }

    @PostMapping("/patients/{id}/consents")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Consent signed at the spa")
    public Consent grant(@PathVariable Long id, @Valid @RequestBody ConsentRequest req,
                         @RequestAttribute(PanelAuthorizationManager.MEMBER_ATTRIBUTE) TenantMember m) {
        return consents.grant(id, req.type(), req.treatmentId(), ConsentChannel.IN_PERSON, actor(m).name());
    }

    @DeleteMapping("/patients/{id}/consents/{type}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable Long id, @PathVariable ConsentType type) {
        consents.revoke(id, type);
    }

    @GetMapping("/patients/{id}/access-log")
    public List<ClinicalAccess> accessLog(@PathVariable Long id) {
        return records.accessLog(id);
    }

    private static Actor actor(TenantMember member) {
        return Actors.staff(member, AccessChannel.PANEL);
    }
}
