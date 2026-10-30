package com.lsoftware.spaco.usecase;

import com.lsoftware.spaco.entity.appointment.model.Appointment;
import com.lsoftware.spaco.entity.appointment.model.AppointmentStatus;
import com.lsoftware.spaco.entity.patient.model.Patient;
import com.lsoftware.spaco.entity.professional.model.Professional;
import com.lsoftware.spaco.entity.tenant.model.Tenant;
import com.lsoftware.spaco.entity.treatment.model.Treatment;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Fixtures {

    public static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    /** Tuesday 2026-10-20, 8:00 in Bogotá */
    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-20T13:00:00Z"), ZoneOffset.UTC);
    public static final Tenant SPA = new Tenant(1L, "miel-spa", "Miel Spa", BOGOTA, null, null, 30, 120, Map.of());

    private Fixtures() {}

    public static Treatment treatment(long id, int durationMin, int bufferMin) {
        return new Treatment(id, 1L, "faciales", "Faciales", 5, "Facial hidratante con miel", "facial-" + id, null,
                null, List.of(), List.of(), "Embarazo, alergia a la miel, rosácea activa", durationMin, bufferMin,
                null, null, 1, true);
    }

    public static Professional professional(long id) {
        return new Professional(id, "Profesional " + id, "Esteticista", null, null, null, true);
    }

    public static Patient patient(long id) {
        return new Patient(id, "Laura Gómez", "+573001112233", "laura@example.com", null, null, null);
    }

    public static OffsetDateTime at(String localDateTime) {
        return java.time.LocalDateTime.parse(localDateTime).atZone(BOGOTA).toOffsetDateTime();
    }

    public static Appointment appointment(long id, long professionalId, long patientId, OffsetDateTime start,
                                          int minutes, AppointmentStatus status) {
        return new Appointment(id, 5L, "Facial", professionalId, "Profesional", patientId, "Laura Gómez",
                "+573001112233", start, start.plusMinutes(minutes), status, null, UUID.randomUUID(), null, null, null);
    }
}
