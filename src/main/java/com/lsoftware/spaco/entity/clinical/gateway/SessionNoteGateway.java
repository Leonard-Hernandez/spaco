package com.lsoftware.spaco.entity.clinical.gateway;

import com.lsoftware.spaco.entity.clinical.model.NoteMatch;
import com.lsoftware.spaco.entity.clinical.model.PhotoKind;
import com.lsoftware.spaco.entity.clinical.model.SessionNote;
import com.lsoftware.spaco.entity.clinical.model.SessionPhoto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionNoteGateway {
    SessionNote save(SessionNote note);
    Optional<SessionNote> findById(Long id);
    Optional<SessionNote> findByAppointment(Long appointmentId);
    /** Newest first, optionally within [from, to). */
    List<SessionNote> findByPatient(Long patientId, OffsetDateTime from, OffsetDateTime to);
    SessionPhoto addPhoto(Long sessionNoteId, PhotoKind kind, String fileName, String caption);
    Optional<SessionPhoto> findPhoto(Long photoId);
    void deletePhoto(Long photoId);
    /** Spanish full-text search over products, parameters, observations and recommendations. */
    List<NoteMatch> search(String text, Long patientId, int limit);
}
