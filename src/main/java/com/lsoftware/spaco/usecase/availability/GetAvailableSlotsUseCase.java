package com.lsoftware.spaco.usecase.availability;

import com.lsoftware.spaco.entity.appointment.gateway.AppointmentGateway;
import com.lsoftware.spaco.entity.appointment.model.Appointment;
import com.lsoftware.spaco.entity.availability.gateway.AvailabilityExceptionGateway;
import com.lsoftware.spaco.entity.availability.gateway.AvailabilityRuleGateway;
import com.lsoftware.spaco.entity.availability.model.AvailabilityException;
import com.lsoftware.spaco.entity.availability.model.AvailabilityRule;
import com.lsoftware.spaco.entity.availability.model.ExceptionType;
import com.lsoftware.spaco.entity.availability.model.TimeWindow;
import com.lsoftware.spaco.entity.professional.exception.ProfessionalNotFoundException;
import com.lsoftware.spaco.entity.professional.gateway.ProfessionalGateway;
import com.lsoftware.spaco.entity.professional.model.Professional;
import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.entity.tenant.model.Tenant;
import com.lsoftware.spaco.entity.treatment.exception.TreatmentNotFoundException;
import com.lsoftware.spaco.entity.treatment.gateway.TreatmentGateway;
import com.lsoftware.spaco.entity.treatment.model.Treatment;
import com.lsoftware.spaco.usecase.availability.dto.SlotsResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

/** Start times a treatment can be booked at, from the professionals' weekly hours and exceptions. */
@Component
@RequiredArgsConstructor
public class GetAvailableSlotsUseCase {

    private final TreatmentGateway treatments;
    private final ProfessionalGateway professionals;
    private final AvailabilityRuleGateway rules;
    private final AvailabilityExceptionGateway exceptions;
    private final AppointmentGateway appointments;
    private final CurrentTenantGateway tenants;
    private final Clock clock;

    /** With professionalId null, the union over all active professionals. */
    public SlotsResult execute(Long treatmentId, Long professionalId, LocalDate date) {
        Treatment treatment = activeTreatment(treatmentId);
        TreeSet<LocalTime> slots = new TreeSet<>();
        for (Professional professional : candidates(professionalId)) {
            slots.addAll(slotsFor(treatment, professional.id(), date));
        }
        return new SlotsResult(date, treatmentId, professionalId, treatment.durationMin(), List.copyOf(slots));
    }

    /** First professional (or the requested one) free for the treatment at that exact start. */
    public Optional<Professional> findFreeProfessional(Long treatmentId, Long professionalId, OffsetDateTime startAt) {
        Treatment treatment = activeTreatment(treatmentId);
        ZoneId tz = tenants.current().timezone();
        LocalDate date = startAt.atZoneSameInstant(tz).toLocalDate();
        LocalTime time = startAt.atZoneSameInstant(tz).toLocalTime();
        return candidates(professionalId).stream()
                .filter(p -> slotsFor(treatment, p.id(), date).contains(time))
                .findFirst();
    }

    /** Earliest start a patient can request at this spa. */
    public OffsetDateTime earliestStart(Tenant tenant) {
        return OffsetDateTime.now(clock).plusMinutes(tenant.minBookingLeadMin());
    }

    private Treatment activeTreatment(Long treatmentId) {
        return treatments.findById(treatmentId)
                .filter(Treatment::active)
                .orElseThrow(TreatmentNotFoundException::new);
    }

    private List<Professional> candidates(Long professionalId) {
        if (professionalId == null) return professionals.findAllActive();
        return List.of(professionals.findById(professionalId)
                .filter(Professional::active)
                .orElseThrow(ProfessionalNotFoundException::new));
    }

    private List<LocalTime> slotsFor(Treatment treatment, Long professionalId, LocalDate date) {
        Tenant tenant = tenants.current();
        int busy = treatment.blockMin();
        ZoneId tz = tenant.timezone();
        OffsetDateTime earliest = earliestStart(tenant);

        OffsetDateTime dayStart = date.atStartOfDay(tz).toOffsetDateTime();
        OffsetDateTime dayEnd = date.plusDays(1).atStartOfDay(tz).toOffsetDateTime();
        List<Appointment> booked = appointments.findActiveOverlapping(professionalId, dayStart, dayEnd);

        List<LocalTime> slots = new ArrayList<>();
        for (TimeWindow w : windows(professionalId, date)) {
            LocalDateTime cursor = LocalDateTime.of(date, w.start());
            LocalDateTime windowEnd = LocalDateTime.of(date, w.end());
            while (!cursor.plusMinutes(busy).isAfter(windowEnd)) {
                OffsetDateTime start = cursor.atZone(tz).toOffsetDateTime();
                OffsetDateTime end = start.plusMinutes(busy);
                if (!start.isBefore(earliest) && booked.stream().noneMatch(a -> overlaps(start, end, a))) {
                    slots.add(cursor.toLocalTime());
                }
                cursor = cursor.plusMinutes(tenant.slotStepMin());
            }
        }
        return slots;
    }

    /** Weekly hours of that weekday, plus EXTRA windows, minus BLOCKs. */
    List<TimeWindow> windows(Long professionalId, LocalDate date) {
        List<TimeWindow> windows = new ArrayList<>(rules.findActiveByProfessionalAndWeekday(professionalId, date.getDayOfWeek())
                .stream()
                .filter(AvailabilityRule::active)
                .map(r -> new TimeWindow(r.startTime(), r.endTime()))
                .toList());
        List<AvailabilityException> dayExceptions = exceptions.findForProfessionalOnDate(professionalId, date);
        dayExceptions.stream()
                .filter(e -> e.type() == ExceptionType.EXTRA && !e.wholeDay())
                .forEach(e -> windows.add(new TimeWindow(e.startTime(), e.endTime())));
        List<TimeWindow> result = windows;
        for (AvailabilityException block : dayExceptions) {
            if (block.type() != ExceptionType.BLOCK) continue;
            if (block.wholeDay()) return List.of();
            result = subtract(result, block.startTime(), block.endTime());
        }
        return result.stream()
                .filter(w -> w.start().isBefore(w.end()))
                .sorted(Comparator.comparing(TimeWindow::start))
                .toList();
    }

    private static List<TimeWindow> subtract(List<TimeWindow> windows, LocalTime from, LocalTime to) {
        List<TimeWindow> result = new ArrayList<>();
        for (TimeWindow w : windows) {
            if (!w.start().isBefore(to) || !w.end().isAfter(from)) {
                result.add(w);
                continue;
            }
            if (w.start().isBefore(from)) result.add(new TimeWindow(w.start(), from));
            if (w.end().isAfter(to)) result.add(new TimeWindow(to, w.end()));
        }
        return result;
    }

    private static boolean overlaps(OffsetDateTime start, OffsetDateTime end, Appointment a) {
        return start.isBefore(a.endAt()) && end.isAfter(a.startAt());
    }
}
