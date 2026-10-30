package com.lsoftware.spaco.usecase.clinical;

import com.lsoftware.spaco.entity.appointment.gateway.AppointmentGateway;
import com.lsoftware.spaco.entity.appointment.model.Appointment;
import com.lsoftware.spaco.entity.clinical.gateway.ClinicalRecordGateway;
import com.lsoftware.spaco.entity.clinical.gateway.SessionNoteGateway;
import com.lsoftware.spaco.entity.clinical.model.Actor;
import com.lsoftware.spaco.entity.clinical.model.ClinicalRecord;
import com.lsoftware.spaco.entity.clinical.model.NoteMatch;
import com.lsoftware.spaco.entity.consent.gateway.ConsentGateway;
import com.lsoftware.spaco.entity.consent.model.Consent;
import com.lsoftware.spaco.entity.consent.model.ConsentType;
import com.lsoftware.spaco.entity.patient.exception.PatientNotFoundException;
import com.lsoftware.spaco.entity.patient.gateway.PatientGateway;
import com.lsoftware.spaco.entity.patient.model.Patient;
import com.lsoftware.spaco.entity.treatment.exception.TreatmentNotFoundException;
import com.lsoftware.spaco.entity.treatment.gateway.TreatmentGateway;
import com.lsoftware.spaco.entity.treatment.model.Treatment;
import com.lsoftware.spaco.usecase.clinical.dto.ContraindicationCheck;
import com.lsoftware.spaco.usecase.clinical.dto.PatientSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Questions about a patient's history, for the panel and the assistant (MCP). */
@Component
@RequiredArgsConstructor
public class PatientSummaryUseCase {

    static final int RECENT_SESSIONS = 3;
    static final int MAX_MATCHES = 20;

    private final PatientGateway patients;
    private final ClinicalRecordGateway records;
    private final SessionNoteGateway notes;
    private final AppointmentGateway appointments;
    private final ConsentGateway consents;
    private final TreatmentGateway treatments;
    private final ClinicalAudit audit;
    private final Clock clock;

    @Transactional
    public PatientSummary summary(Long patientId, Actor actor) {
        Patient patient = patients.findById(patientId).orElseThrow(PatientNotFoundException::new);
        audit.record(patientId, actor, "READ_SUMMARY");
        ClinicalRecord record = records.findByPatient(patientId).orElseGet(() -> ClinicalRecord.empty(patientId));
        Set<ConsentType> valid = validConsents(patientId);

        List<String> alerts = new ArrayList<>(ClinicalAlerts.of(record));
        if (!record.filled()) alerts.add("Sin anamnesis registrada");
        if (!valid.contains(ConsentType.DATA_PROCESSING)) alerts.add("Sin autorización de tratamiento de datos");

        OffsetDateTime now = OffsetDateTime.now(clock);
        List<Appointment> upcoming = appointments.findByPatient(patientId).stream()
                .filter(a -> a.status().holdsSlot() && a.startAt().isAfter(now))
                .sorted(Comparator.comparing(Appointment::startAt))
                .toList();
        var recent = notes.findByPatient(patientId, null, null).stream().limit(RECENT_SESSIONS).toList();
        return new PatientSummary(patient, age(patient.birthDate()), record, alerts, recent, upcoming, valid);
    }

    @Transactional
    public ContraindicationCheck checkContraindications(Long patientId, Long treatmentId, Actor actor) {
        Patient patient = patients.findById(patientId).orElseThrow(PatientNotFoundException::new);
        Treatment treatment = treatments.findById(treatmentId).orElseThrow(TreatmentNotFoundException::new);
        audit.record(patientId, actor, "CHECK_CONTRAINDICATIONS");
        ClinicalRecord record = records.findByPatient(patientId).orElseGet(() -> ClinicalRecord.empty(patientId));
        boolean signed = consents.findValid(patientId, ConsentType.TREATMENT).stream()
                .anyMatch(c -> treatmentId.equals(c.treatmentId()));
        return new ContraindicationCheck(patientId, patient.fullName(), treatmentId, treatment.name(),
                treatment.contraindications(), ClinicalAlerts.matches(treatment.contraindications(), record),
                ClinicalAlerts.of(record), record.filled(), signed);
    }

    /** Full-text search over session notes, of one patient or the whole spa. */
    @Transactional
    public List<NoteMatch> searchNotes(String text, Long patientId, Actor actor) {
        audit.record(patientId, actor, "SEARCH_NOTES");
        return notes.search(text, patientId, MAX_MATCHES);
    }

    private Set<ConsentType> validConsents(Long patientId) {
        Set<ConsentType> valid = EnumSet.noneOf(ConsentType.class);
        consents.findByPatient(patientId).stream().filter(Consent::valid).map(Consent::type).forEach(valid::add);
        return valid;
    }

    private Integer age(LocalDate birthDate) {
        return birthDate == null ? null : Period.between(birthDate, LocalDate.now(clock)).getYears();
    }
}
