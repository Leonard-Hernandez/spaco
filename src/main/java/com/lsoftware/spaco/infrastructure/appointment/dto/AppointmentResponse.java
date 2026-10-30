package com.lsoftware.spaco.infrastructure.appointment.dto;

import com.lsoftware.spaco.entity.appointment.model.Appointment;
import com.lsoftware.spaco.entity.appointment.model.AppointmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentResponse(
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
        OffsetDateTime decidedAt,
        String rejectReason,
        OffsetDateTime createdAt
) {
    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(a.id(), a.treatmentId(), a.treatmentName(), a.professionalId(),
                a.professionalName(), a.patientId(), a.patientName(), a.patientPhone(), a.startAt(), a.endAt(),
                a.status(), a.patientNote(), a.decidedAt(), a.rejectReason(), a.createdAt());
    }
}
