package com.lsoftware.spaco.infrastructure.appointment.dto;

import com.lsoftware.spaco.entity.appointment.model.Appointment;
import com.lsoftware.spaco.entity.appointment.model.AppointmentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** What the patient sees of her booking; token = the link to manage it. */
public record BookingResponse(
        UUID token,
        Long treatmentId,
        String treatmentName,
        String professionalName,
        String patientName,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        AppointmentStatus status,
        String rejectReason
) {
    public static BookingResponse from(Appointment a) {
        return new BookingResponse(a.managementToken(), a.treatmentId(), a.treatmentName(), a.professionalName(),
                a.patientName(), a.startAt(), a.endAt(), a.status(), a.rejectReason());
    }
}
