package com.lsoftware.spaco.entity.appointment.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Appointment(
        Long id,
        Long treatmentId,
        String treatmentName,
        Long professionalId,
        String professionalName,
        Long patientId,
        String patientName,
        String patientPhone,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        AppointmentStatus status,
        String patientNote,
        UUID managementToken,
        OffsetDateTime decidedAt,
        String rejectReason,
        OffsetDateTime createdAt
) {
    public Appointment withStatus(AppointmentStatus newStatus, OffsetDateTime at, String reason) {
        return new Appointment(id, treatmentId, treatmentName, professionalId, professionalName, patientId,
                patientName, patientPhone, startAt, endAt, newStatus, patientNote, managementToken,
                at, reason, createdAt);
    }
}
