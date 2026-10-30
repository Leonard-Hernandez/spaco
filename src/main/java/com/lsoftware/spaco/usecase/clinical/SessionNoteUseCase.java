package com.lsoftware.spaco.usecase.clinical;

import com.lsoftware.spaco.entity.appointment.gateway.AppointmentGateway;
import com.lsoftware.spaco.entity.appointment.model.Appointment;
import com.lsoftware.spaco.entity.appointment.model.AppointmentStatus;
import com.lsoftware.spaco.entity.clinical.exception.PhotoNotFoundException;
import com.lsoftware.spaco.entity.clinical.exception.SessionNoteNotFoundException;
import com.lsoftware.spaco.entity.clinical.gateway.SessionNoteGateway;
import com.lsoftware.spaco.entity.clinical.model.Actor;
import com.lsoftware.spaco.entity.clinical.model.PhotoKind;
import com.lsoftware.spaco.entity.clinical.model.SessionNote;
import com.lsoftware.spaco.entity.clinical.model.SessionPhoto;
import com.lsoftware.spaco.entity.exception.ConflictException;
import com.lsoftware.spaco.entity.media.gateway.ClinicalPhotoGateway;
import com.lsoftware.spaco.entity.patient.exception.PatientNotFoundException;
import com.lsoftware.spaco.entity.patient.gateway.PatientGateway;
import com.lsoftware.spaco.entity.professional.gateway.ProfessionalGateway;
import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.entity.treatment.gateway.TreatmentGateway;
import com.lsoftware.spaco.usecase.clinical.dto.PhotoFile;
import com.lsoftware.spaco.usecase.clinical.dto.SessionNoteCommand;
import com.lsoftware.spaco.usecase.media.ImageFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import static com.lsoftware.spaco.usecase.clinical.ClinicalRecordUseCase.trim;

/** What was done in each visit, with its before/after photos (private). */
@Component
@RequiredArgsConstructor
public class SessionNoteUseCase {

    private final SessionNoteGateway notes;
    private final PatientGateway patients;
    private final AppointmentGateway appointments;
    private final TreatmentGateway treatments;
    private final ProfessionalGateway professionals;
    private final ClinicalPhotoGateway photos;
    private final CurrentTenantGateway tenants;
    private final ClinicalAudit audit;
    private final Clock clock;

    @Transactional
    public List<SessionNote> list(Long patientId, OffsetDateTime from, OffsetDateTime to, Actor actor) {
        patients.findById(patientId).orElseThrow(PatientNotFoundException::new);
        audit.record(patientId, actor, "READ_SESSIONS");
        return notes.findByPatient(patientId, from, to);
    }

    @Transactional
    public SessionNote get(Long id, Actor actor) {
        SessionNote note = notes.findById(id).orElseThrow(SessionNoteNotFoundException::new);
        audit.record(note.patientId(), actor, "READ_SESSION");
        return note;
    }

    /** With an appointment: that visit's note (one per appointment), and the visit is completed. */
    @Transactional
    public SessionNote create(Long patientId, SessionNoteCommand cmd, Actor actor) {
        patients.findById(patientId).orElseThrow(PatientNotFoundException::new);
        Long treatmentId = cmd.treatmentId();
        Long professionalId = cmd.professionalId();
        OffsetDateTime performedAt = cmd.performedAt() != null ? cmd.performedAt() : OffsetDateTime.now(clock);
        if (cmd.appointmentId() != null) {
            Appointment appointment = appointments.findById(cmd.appointmentId())
                    .filter(a -> a.patientId().equals(patientId))
                    .orElseThrow(() -> new ConflictException("APPOINTMENT_MISMATCH", "That appointment is not this patient's"));
            if (notes.findByAppointment(appointment.id()).isPresent()) {
                throw new ConflictException("SESSION_EXISTS", "That visit already has a note; edit it");
            }
            if (appointment.status() == AppointmentStatus.PENDING || appointment.status() == AppointmentStatus.ACCEPTED) {
                appointments.save(appointment.withStatus(AppointmentStatus.COMPLETED, OffsetDateTime.now(clock), null));
            }
            if (treatmentId == null) treatmentId = appointment.treatmentId();
            if (professionalId == null) professionalId = appointment.professionalId();
            if (cmd.performedAt() == null) performedAt = appointment.startAt();
        }
        audit.record(patientId, actor, "CREATE_SESSION");
        return notes.save(note(null, patientId, cmd.appointmentId(), treatmentId, professionalId, performedAt, cmd,
                actor.name(), List.of()));
    }

    @Transactional
    public SessionNote update(Long id, SessionNoteCommand cmd, Actor actor) {
        SessionNote current = notes.findById(id).orElseThrow(SessionNoteNotFoundException::new);
        audit.record(current.patientId(), actor, "UPDATE_SESSION");
        return notes.save(note(id, current.patientId(), current.appointmentId(),
                cmd.treatmentId() != null ? cmd.treatmentId() : current.treatmentId(),
                cmd.professionalId() != null ? cmd.professionalId() : current.professionalId(),
                cmd.performedAt() != null ? cmd.performedAt() : current.performedAt(),
                cmd, current.createdBy(), current.photos()));
    }

    @Transactional
    public SessionPhoto addPhoto(Long noteId, PhotoKind kind, byte[] content, String caption, Actor actor) {
        SessionNote note = notes.findById(noteId).orElseThrow(SessionNoteNotFoundException::new);
        String ext = ImageFormat.extensionOf(content);
        String file = photos.store(tenants.current().slug(), ImageFormat.withoutMetadata(content, ext), ext);
        audit.record(note.patientId(), actor, "ADD_PHOTO");
        return notes.addPhoto(noteId, kind, file, trim(caption));
    }

    @Transactional
    public void deletePhoto(Long photoId, Actor actor) {
        PhotoFile file = photo(photoId, null);
        audit.record(patientOf(file.photo()), actor, "DELETE_PHOTO");
        notes.deletePhoto(photoId);
        photos.delete(tenants.current().slug(), file.photo().fileName());
    }

    /**
     * The photo file. patientId set = only if it belongs to that patient (her own portal).
     * Viewing is not audited per image: opening the session already was.
     */
    @Transactional(readOnly = true)
    public PhotoFile photo(Long photoId, Long patientId) {
        SessionPhoto photo = notes.findPhoto(photoId).orElseThrow(PhotoNotFoundException::new);
        if (patientId != null && !patientId.equals(patientOf(photo))) throw new PhotoNotFoundException();
        return new PhotoFile(photo, photos.find(tenants.current().slug(), photo.fileName())
                .orElseThrow(PhotoNotFoundException::new));
    }

    private Long patientOf(SessionPhoto photo) {
        return notes.findById(photo.sessionNoteId()).map(SessionNote::patientId).orElseThrow(PhotoNotFoundException::new);
    }

    private SessionNote note(Long id, Long patientId, Long appointmentId, Long treatmentId, Long professionalId,
                             OffsetDateTime performedAt, SessionNoteCommand cmd, String createdBy,
                             List<SessionPhoto> existingPhotos) {
        String treatmentName = treatmentId == null ? null : treatments.findById(treatmentId).map(t -> t.name()).orElse(null);
        String professionalName = professionalId == null ? null
                : professionals.findById(professionalId).map(p -> p.displayName()).orElse(null);
        return new SessionNote(id, patientId, appointmentId, treatmentId, treatmentName, professionalId,
                professionalName, performedAt, trim(cmd.products()), trim(cmd.parameters()), trim(cmd.observations()),
                trim(cmd.recommendations()), createdBy, existingPhotos);
    }
}
