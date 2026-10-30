package com.lsoftware.spaco.infrastructure.config.db.mapper;

import com.lsoftware.spaco.entity.clinical.model.AccessChannel;
import com.lsoftware.spaco.entity.clinical.model.ClinicalAccess;
import com.lsoftware.spaco.entity.clinical.model.ClinicalRecord;
import com.lsoftware.spaco.entity.clinical.model.PhotoKind;
import com.lsoftware.spaco.entity.clinical.model.SessionNote;
import com.lsoftware.spaco.entity.clinical.model.SessionPhoto;
import com.lsoftware.spaco.entity.consent.model.Consent;
import com.lsoftware.spaco.entity.consent.model.ConsentChannel;
import com.lsoftware.spaco.entity.consent.model.ConsentTemplate;
import com.lsoftware.spaco.entity.consent.model.ConsentType;
import com.lsoftware.spaco.infrastructure.config.db.schema.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClinicalMapper {

    public ClinicalRecord toDomain(ClinicalRecordEntity e) {
        return new ClinicalRecord(e.getPatientId(), e.getAllergies(), e.getMedications(), e.getConditions(),
                e.getPregnancy(), e.getSkinType(), e.getPhototype() == null ? null : e.getPhototype().intValue(),
                e.getSurgeries(), e.getHabits(), e.getObservations(), e.getUpdatedBy(), e.getUpdatedAt());
    }

    public ClinicalRecordEntity toEntity(ClinicalRecord r) {
        return ClinicalRecordEntity.builder()
                .patientId(r.patientId()).allergies(r.allergies()).medications(r.medications())
                .conditions(r.conditions()).pregnancy(r.pregnancy()).skinType(r.skinType())
                .phototype(r.phototype() == null ? null : r.phototype().shortValue()).surgeries(r.surgeries())
                .habits(r.habits()).observations(r.observations()).updatedBy(r.updatedBy()).updatedAt(r.updatedAt())
                .build();
    }

    public SessionNote toDomain(SessionNoteEntity e, String treatmentName, String professionalName,
                                List<SessionPhoto> photos) {
        return new SessionNote(e.getId(), e.getPatientId(), e.getAppointmentId(), e.getTreatmentId(), treatmentName,
                e.getProfessionalId(), professionalName, e.getPerformedAt(), e.getProducts(), e.getParameters(),
                e.getObservations(), e.getRecommendations(), e.getCreatedBy(), photos);
    }

    public SessionNoteEntity toEntity(SessionNote n) {
        return SessionNoteEntity.builder()
                .id(n.id()).patientId(n.patientId()).appointmentId(n.appointmentId()).treatmentId(n.treatmentId())
                .professionalId(n.professionalId()).performedAt(n.performedAt()).products(n.products())
                .parameters(n.parameters()).observations(n.observations()).recommendations(n.recommendations())
                .createdBy(n.createdBy())
                .build();
    }

    public SessionPhoto toDomain(SessionPhotoEntity e) {
        return new SessionPhoto(e.getId(), e.getSessionNoteId(), PhotoKind.valueOf(e.getKind()), e.getFileName(),
                e.getCaption(), e.getCreatedAt());
    }

    public Consent toDomain(ConsentEntity e) {
        return new Consent(e.getId(), e.getPatientId(), ConsentType.valueOf(e.getType()), e.getTemplateVersion(),
                e.getTreatmentId(), e.getAcceptedAt(), e.getRevokedAt(), ConsentChannel.valueOf(e.getChannel()),
                e.getRecordedBy());
    }

    public ConsentEntity toEntity(Consent c) {
        return ConsentEntity.builder()
                .id(c.id()).patientId(c.patientId()).type(c.type().name()).templateVersion(c.templateVersion())
                .treatmentId(c.treatmentId()).acceptedAt(c.acceptedAt()).revokedAt(c.revokedAt())
                .channel(c.channel().name()).recordedBy(c.recordedBy())
                .build();
    }

    public ConsentTemplate toDomain(ConsentTemplateEntity e) {
        return new ConsentTemplate(e.getId(), ConsentType.valueOf(e.getType()), e.getVersion(), e.getTitle(),
                e.getBody(), e.isActive());
    }

    public ClinicalAccess toDomain(ClinicalAccessEntity e) {
        return new ClinicalAccess(e.getId(), e.getPatientId(), e.getActorSub(), e.getActorName(),
                AccessChannel.valueOf(e.getChannel()), e.getAction(), e.getAt());
    }
}
