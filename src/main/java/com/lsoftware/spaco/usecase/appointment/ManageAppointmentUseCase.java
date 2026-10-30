package com.lsoftware.spaco.usecase.appointment;

import com.lsoftware.spaco.entity.appointment.exception.AppointmentNotFoundException;
import com.lsoftware.spaco.entity.appointment.exception.InvalidAppointmentStateException;
import com.lsoftware.spaco.entity.appointment.gateway.AppointmentGateway;
import com.lsoftware.spaco.entity.appointment.model.Appointment;
import com.lsoftware.spaco.entity.appointment.model.AppointmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.lsoftware.spaco.entity.appointment.model.AppointmentStatus.*;

/** The life of an appointment, for the staff (agenda, requests) and the patient's link. */
@Component
@RequiredArgsConstructor
public class ManageAppointmentUseCase {

    private final AppointmentGateway appointments;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<Appointment> list(OffsetDateTime from, OffsetDateTime to, AppointmentStatus status, Long professionalId) {
        return appointments.findByDateRange(from, to, status, professionalId);
    }

    @Transactional(readOnly = true)
    public List<Appointment> pending() {
        return appointments.findByStatus(PENDING);
    }

    @Transactional(readOnly = true)
    public List<Appointment> ofPatient(Long patientId) {
        return appointments.findByPatient(patientId);
    }

    @Transactional(readOnly = true)
    public Appointment get(Long id) {
        return appointments.findById(id).orElseThrow(AppointmentNotFoundException::new);
    }

    @Transactional
    public Appointment accept(Long id) {
        return move(get(id), Set.of(PENDING), ACCEPTED, null);
    }

    @Transactional
    public Appointment reject(Long id, String reason) {
        return move(get(id), Set.of(PENDING), REJECTED, reason);
    }

    @Transactional
    public Appointment cancel(Long id, String reason) {
        return move(get(id), Set.of(PENDING, ACCEPTED), CANCELLED, reason);
    }

    /** The patient came (walk-ins that were still pending count too). */
    @Transactional
    public Appointment complete(Long id) {
        Appointment appointment = get(id);
        if (appointment.status() == COMPLETED) return appointment;
        return move(appointment, Set.of(PENDING, ACCEPTED), COMPLETED, null);
    }

    @Transactional
    public Appointment noShow(Long id) {
        Appointment appointment = get(id);
        if (appointment.startAt().isAfter(OffsetDateTime.now(clock))) throw new InvalidAppointmentStateException();
        return move(appointment, Set.of(ACCEPTED), NO_SHOW, null);
    }

    @Transactional(readOnly = true)
    public Appointment byToken(UUID token) {
        return appointments.findByManagementToken(token).orElseThrow(AppointmentNotFoundException::new);
    }

    /** From the patient's link: only before it starts. */
    @Transactional
    public Appointment cancelByToken(UUID token) {
        Appointment appointment = byToken(token);
        if (!appointment.startAt().isAfter(OffsetDateTime.now(clock))) throw new InvalidAppointmentStateException();
        return move(appointment, Set.of(PENDING, ACCEPTED), CANCELLED, "Cancelada por la paciente");
    }

    /** Requests nobody answered before their start time free nothing anymore: close them. */
    @Transactional
    public int expireUnanswered() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        List<Appointment> stale = appointments.findByStatus(PENDING).stream()
                .filter(a -> a.startAt().isBefore(now))
                .toList();
        stale.forEach(a -> appointments.save(a.withStatus(REJECTED, now, "Sin respuesta del spa")));
        return stale.size();
    }

    private Appointment move(Appointment appointment, Set<AppointmentStatus> from, AppointmentStatus to, String reason) {
        if (!from.contains(appointment.status())) throw new InvalidAppointmentStateException();
        return appointments.save(appointment.withStatus(to, OffsetDateTime.now(clock), reason));
    }
}
