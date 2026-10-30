package com.lsoftware.spaco.usecase.appointment;

import com.lsoftware.spaco.entity.appointment.exception.SlotTakenException;
import com.lsoftware.spaco.entity.appointment.exception.TooManyPendingException;
import com.lsoftware.spaco.entity.appointment.gateway.AppointmentGateway;
import com.lsoftware.spaco.entity.appointment.model.Appointment;
import com.lsoftware.spaco.entity.appointment.model.AppointmentStatus;
import com.lsoftware.spaco.entity.consent.exception.ConsentRequiredException;
import com.lsoftware.spaco.entity.consent.model.ConsentChannel;
import com.lsoftware.spaco.entity.consent.model.ConsentType;
import com.lsoftware.spaco.entity.exception.DomainException;
import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.entity.treatment.gateway.TreatmentGateway;
import com.lsoftware.spaco.usecase.appointment.dto.BookingCommand;
import com.lsoftware.spaco.usecase.availability.GetAvailableSlotsUseCase;
import com.lsoftware.spaco.usecase.consent.ConsentUseCase;
import com.lsoftware.spaco.usecase.patient.ProvisionPatientUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.lsoftware.spaco.usecase.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookAppointmentUseCaseTest {

    private static final OffsetDateTime START = at("2026-10-21T09:00");

    private final TreatmentGateway treatments = mock(TreatmentGateway.class);
    private final AppointmentGateway appointments = mock(AppointmentGateway.class);
    private final GetAvailableSlotsUseCase slots = mock(GetAvailableSlotsUseCase.class);
    private final ProvisionPatientUseCase provision = mock(ProvisionPatientUseCase.class);
    private final ConsentUseCase consents = mock(ConsentUseCase.class);
    private final CurrentTenantGateway tenants = mock(CurrentTenantGateway.class);
    private final BookAppointmentUseCase useCase = new BookAppointmentUseCase(treatments, appointments, slots,
            provision, consents, tenants, CLOCK);

    @BeforeEach
    void setUp() {
        when(tenants.current()).thenReturn(SPA);
        when(treatments.findById(5L)).thenReturn(Optional.of(treatment(5L, 50, 10)));
        when(slots.earliestStart(SPA)).thenReturn(OffsetDateTime.now(CLOCK).plusMinutes(120));
        when(provision.execute(any(), any(), any())).thenReturn(patient(3L));
        when(appointments.findByPatient(3L)).thenReturn(List.of());
        when(slots.findFreeProfessional(5L, null, START)).thenReturn(Optional.of(professional(2L)));
        when(appointments.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void aGuestBookingHoldsTheSlotAndRecordsTheDataConsent() {
        Appointment booked = useCase.execute(command(START, true));

        assertThat(booked.status()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(booked.professionalId()).isEqualTo(2L);
        assertThat(booked.endAt()).isEqualTo(START.plusMinutes(60));
        assertThat(booked.patientNote()).isEqualTo("Piel sensible");
        verify(consents).grant(3L, ConsentType.DATA_PROCESSING, null, ConsentChannel.WEB, null);
    }

    @Test
    void theDataPolicyMustBeAccepted() {
        assertThatThrownBy(() -> useCase.execute(command(START, false))).isInstanceOf(ConsentRequiredException.class);
        verifyNoInteractions(provision, appointments);
    }

    @Test
    void tooCloseStartsAreRejected() {
        assertThatThrownBy(() -> useCase.execute(command(at("2026-10-20T09:00"), true)))
                .isInstanceOf(DomainException.class).hasFieldOrPropertyWithValue("code", "TOO_SOON");
    }

    @Test
    void aPhoneCannotHoldMoreThanThreePendingRequests() {
        when(appointments.findByPatient(3L)).thenReturn(List.of(
                appointment(1L, 1L, 3L, START.plusDays(1), 60, AppointmentStatus.PENDING),
                appointment(2L, 1L, 3L, START.plusDays(2), 60, AppointmentStatus.PENDING),
                appointment(3L, 1L, 3L, START.plusDays(3), 60, AppointmentStatus.PENDING)));

        assertThatThrownBy(() -> useCase.execute(command(START, true))).isInstanceOf(TooManyPendingException.class);
    }

    @Test
    void aTakenSlotIsReported() {
        when(slots.findFreeProfessional(5L, null, START)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(command(START, true))).isInstanceOf(SlotTakenException.class);
        verify(appointments, never()).save(any());
    }

    private static BookingCommand command(OffsetDateTime start, boolean accepts) {
        return new BookingCommand(5L, null, start, "Laura Gómez", "300 111 2233", "laura@example.com",
                " Piel sensible ", accepts);
    }
}
