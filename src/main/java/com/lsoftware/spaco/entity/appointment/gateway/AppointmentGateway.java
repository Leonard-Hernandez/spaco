package com.lsoftware.spaco.entity.appointment.gateway;

import com.lsoftware.spaco.entity.appointment.model.Appointment;
import com.lsoftware.spaco.entity.appointment.model.AppointmentStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentGateway {
    Appointment save(Appointment appointment);
    Optional<Appointment> findById(Long id);
    Optional<Appointment> findByManagementToken(UUID token);
    /** PENDING or ACCEPTED appointments of a professional overlapping [start, end). */
    List<Appointment> findActiveOverlapping(Long professionalId, OffsetDateTime start, OffsetDateTime end);
    /** Starting in [from, to), optionally only one status and one professional. */
    List<Appointment> findByDateRange(OffsetDateTime from, OffsetDateTime to, AppointmentStatus status, Long professionalId);
    List<Appointment> findByPatient(Long patientId);
    List<Appointment> findByStatus(AppointmentStatus status);
}
