package com.lsoftware.spaco.usecase.availability;

import com.lsoftware.spaco.entity.appointment.gateway.AppointmentGateway;
import com.lsoftware.spaco.entity.appointment.model.AppointmentStatus;
import com.lsoftware.spaco.entity.availability.gateway.AvailabilityExceptionGateway;
import com.lsoftware.spaco.entity.availability.gateway.AvailabilityRuleGateway;
import com.lsoftware.spaco.entity.availability.model.AvailabilityException;
import com.lsoftware.spaco.entity.availability.model.AvailabilityRule;
import com.lsoftware.spaco.entity.availability.model.ExceptionType;
import com.lsoftware.spaco.entity.professional.gateway.ProfessionalGateway;
import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.entity.treatment.gateway.TreatmentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static com.lsoftware.spaco.usecase.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetAvailableSlotsUseCaseTest {

    private static final LocalDate WEDNESDAY = LocalDate.parse("2026-10-21");

    private final TreatmentGateway treatments = mock(TreatmentGateway.class);
    private final ProfessionalGateway professionals = mock(ProfessionalGateway.class);
    private final AvailabilityRuleGateway rules = mock(AvailabilityRuleGateway.class);
    private final AvailabilityExceptionGateway exceptions = mock(AvailabilityExceptionGateway.class);
    private final AppointmentGateway appointments = mock(AppointmentGateway.class);
    private final CurrentTenantGateway tenants = mock(CurrentTenantGateway.class);
    private final GetAvailableSlotsUseCase useCase = new GetAvailableSlotsUseCase(treatments, professionals, rules,
            exceptions, appointments, tenants, CLOCK);

    @BeforeEach
    void setUp() {
        when(tenants.current()).thenReturn(SPA);
        // 50 min facial + 10 min to prepare the room = busy 60 min
        when(treatments.findById(5L)).thenReturn(Optional.of(treatment(5L, 50, 10)));
        when(professionals.findAllActive()).thenReturn(List.of(professional(1L), professional(2L)));
        when(professionals.findById(1L)).thenReturn(Optional.of(professional(1L)));
        when(rules.findActiveByProfessionalAndWeekday(any(), eq(DayOfWeek.WEDNESDAY))).thenReturn(List.of(
                new AvailabilityRule(1L, 1L, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), true)));
        when(exceptions.findForProfessionalOnDate(any(), any())).thenReturn(List.of());
        when(appointments.findActiveOverlapping(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void stepsThroughTheWindowLeavingRoomForTheWholeTreatment() {
        var result = useCase.execute(5L, 1L, WEDNESDAY);

        assertThat(result.slots()).extracting(LocalTime::toString)
                .containsExactly("09:00", "09:30", "10:00", "10:30", "11:00");
    }

    @Test
    void bookedAppointmentsBlockOverlappingStarts() {
        when(appointments.findActiveOverlapping(eq(1L), any(), any())).thenReturn(List.of(
                appointment(9L, 1L, 3L, at("2026-10-21T10:00"), 60, AppointmentStatus.ACCEPTED)));

        var result = useCase.execute(5L, 1L, WEDNESDAY);

        assertThat(result.slots()).extracting(LocalTime::toString).containsExactly("09:00", "11:00");
    }

    @Test
    void aPartialBlockCutsTheWindowAndAWholeDayBlockClosesIt() {
        when(exceptions.findForProfessionalOnDate(1L, WEDNESDAY)).thenReturn(List.of(new AvailabilityException(1L, 1L,
                WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), ExceptionType.BLOCK, "Capacitación")));
        assertThat(useCase.execute(5L, 1L, WEDNESDAY).slots()).extracting(LocalTime::toString)
                .containsExactly("09:00", "11:00");

        when(exceptions.findForProfessionalOnDate(1L, WEDNESDAY)).thenReturn(List.of(new AvailabilityException(2L, null,
                WEDNESDAY, null, null, ExceptionType.BLOCK, "Festivo")));
        assertThat(useCase.execute(5L, 1L, WEDNESDAY).slots()).isEmpty();
    }

    @Test
    void startsInsideTheBookingLeadTimeAreNotOffered() {
        // Now is Tuesday 8:00 and the spa needs 2 hours: Tuesday from 10:00 on
        when(rules.findActiveByProfessionalAndWeekday(any(), eq(DayOfWeek.TUESDAY))).thenReturn(List.of(
                new AvailabilityRule(2L, 1L, DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), true)));

        var result = useCase.execute(5L, 1L, LocalDate.parse("2026-10-20"));

        assertThat(result.slots()).extracting(LocalTime::toString).containsExactly("10:00", "10:30", "11:00");
    }

    @Test
    void withoutPreferenceAnyFreeProfessionalTakesTheSlot() {
        when(appointments.findActiveOverlapping(eq(1L), any(), any())).thenReturn(List.of(
                appointment(9L, 1L, 3L, at("2026-10-21T09:00"), 60, AppointmentStatus.PENDING)));

        var free = useCase.findFreeProfessional(5L, null, at("2026-10-21T09:00"));

        assertThat(free).get().extracting(p -> p.id()).isEqualTo(2L);
    }
}
