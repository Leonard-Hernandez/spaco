package com.lsoftware.spaco.usecase.clinical.dto;

import com.lsoftware.spaco.entity.appointment.model.Appointment;
import com.lsoftware.spaco.entity.clinical.model.ClinicalRecord;
import com.lsoftware.spaco.entity.clinical.model.SessionNote;
import com.lsoftware.spaco.entity.consent.model.ConsentType;
import com.lsoftware.spaco.entity.patient.model.Patient;

import java.util.List;
import java.util.Set;

/**
 * Everything the esthetician needs before treating someone: anamnesis, alerts, last sessions
 * and upcoming appointments.
 */
public record PatientSummary(
        Patient patient,
        Integer age,
        ClinicalRecord record,
        List<String> alerts,
        List<SessionNote> recentSessions,
        List<Appointment> upcoming,
        Set<ConsentType> validConsents
) {}
