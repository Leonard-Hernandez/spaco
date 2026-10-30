package com.lsoftware.spaco.usecase.clinical;

import com.lsoftware.spaco.entity.clinical.gateway.ClinicalAccessGateway;
import com.lsoftware.spaco.entity.clinical.gateway.ClinicalRecordGateway;
import com.lsoftware.spaco.entity.clinical.model.Actor;
import com.lsoftware.spaco.entity.clinical.model.ClinicalAccess;
import com.lsoftware.spaco.entity.clinical.model.ClinicalRecord;
import com.lsoftware.spaco.entity.patient.exception.PatientNotFoundException;
import com.lsoftware.spaco.entity.patient.gateway.PatientGateway;
import com.lsoftware.spaco.usecase.clinical.dto.RecordCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

/** The anamnesis of a patient. */
@Component
@RequiredArgsConstructor
public class ClinicalRecordUseCase {

    private final ClinicalRecordGateway records;
    private final PatientGateway patients;
    private final ClinicalAccessGateway accessLog;
    private final ClinicalAudit audit;
    private final Clock clock;

    @Transactional
    public ClinicalRecord get(Long patientId, Actor actor) {
        patients.findById(patientId).orElseThrow(PatientNotFoundException::new);
        audit.record(patientId, actor, "READ_RECORD");
        return records.findByPatient(patientId).orElseGet(() -> ClinicalRecord.empty(patientId));
    }

    @Transactional
    public ClinicalRecord update(Long patientId, RecordCommand cmd, Actor actor) {
        patients.findById(patientId).orElseThrow(PatientNotFoundException::new);
        audit.record(patientId, actor, "UPDATE_RECORD");
        return records.save(new ClinicalRecord(patientId, trim(cmd.allergies()), trim(cmd.medications()),
                trim(cmd.conditions()), trim(cmd.pregnancy()), trim(cmd.skinType()), cmd.phototype(),
                trim(cmd.surgeries()), trim(cmd.habits()), trim(cmd.observations()), actor.name(),
                OffsetDateTime.now(clock)));
    }

    /** Who looked at or changed this record (shown to the staff and the patient). */
    @Transactional(readOnly = true)
    public List<ClinicalAccess> accessLog(Long patientId) {
        return accessLog.findByPatient(patientId, 100);
    }

    static String trim(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
