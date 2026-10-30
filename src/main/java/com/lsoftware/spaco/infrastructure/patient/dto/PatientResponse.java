package com.lsoftware.spaco.infrastructure.patient.dto;

import com.lsoftware.spaco.entity.patient.model.Patient;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** portalLinked = she already opened her history with Google. */
public record PatientResponse(Long id, String fullName, String phone, String email, LocalDate birthDate,
                              boolean portalLinked, OffsetDateTime createdAt) {
    public static PatientResponse from(Patient p) {
        return new PatientResponse(p.id(), p.fullName(), p.phoneE164(), p.email(), p.birthDate(),
                p.authcoSub() != null, p.createdAt());
    }
}
