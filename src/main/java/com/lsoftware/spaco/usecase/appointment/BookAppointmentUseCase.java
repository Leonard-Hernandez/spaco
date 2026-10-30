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
import com.lsoftware.spaco.entity.patient.model.Patient;
import com.lsoftware.spaco.entity.professional.model.Professional;
import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.entity.treatment.exception.TreatmentNotFoundException;
import com.lsoftware.spaco.entity.treatment.gateway.TreatmentGateway;
import com.lsoftware.spaco.entity.treatment.model.Treatment;
import com.lsoftware.spaco.usecase.appointment.dto.BookingCommand;
import com.lsoftware.spaco.usecase.availability.GetAvailableSlotsUseCase;
import com.lsoftware.spaco.usecase.consent.ConsentUseCase;
import com.lsoftware.spaco.usecase.patient.ProvisionPatientUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Booking from the website, without an account. It is created PENDING (already holding the
 * slot) until the spa confirms it; the patient manages it with the link of its token.
 */
@Component
@RequiredArgsConstructor
public class BookAppointmentUseCase {

    static final int MAX_PENDING_PER_PATIENT = 3;

    private final TreatmentGateway treatments;
    private final AppointmentGateway appointments;
    private final GetAvailableSlotsUseCase slots;
    private final ProvisionPatientUseCase provisionPatient;
    private final ConsentUseCase consents;
    private final CurrentTenantGateway tenants;
    private final Clock clock;

    @Transactional
    public Appointment execute(BookingCommand cmd) {
        if (!cmd.acceptsDataPolicy()) {
            throw new ConsentRequiredException("Accept the personal data policy to book");
        }
        Treatment treatment = treatments.findById(cmd.treatmentId())
                .filter(Treatment::active)
                .orElseThrow(TreatmentNotFoundException::new);
        if (cmd.startAt().isBefore(OffsetDateTime.now(clock))) {
            throw new DomainException("PAST_START", "The appointment must start in the future");
        }
        if (cmd.startAt().isBefore(slots.earliestStart(tenants.current()))) {
            throw new DomainException("TOO_SOON", "This time is too close to book; pick a later one");
        }

        Patient patient = provisionPatient.execute(cmd.fullName(), cmd.phone(), cmd.email());
        long pending = appointments.findByPatient(patient.id()).stream()
                .filter(a -> a.status() == AppointmentStatus.PENDING)
                .count();
        if (pending >= MAX_PENDING_PER_PATIENT) throw new TooManyPendingException();

        Professional professional = slots.findFreeProfessional(treatment.id(), cmd.professionalId(), cmd.startAt())
                .orElseThrow(SlotTakenException::new);
        consents.grant(patient.id(), ConsentType.DATA_PROCESSING, null, ConsentChannel.WEB, null);

        // The exclusion constraint settles two requests racing for the same slot
        return appointments.save(new Appointment(null, treatment.id(), treatment.name(), professional.id(),
                professional.displayName(), patient.id(), patient.fullName(), patient.phoneE164(), cmd.startAt(),
                cmd.startAt().plusMinutes(treatment.blockMin()), AppointmentStatus.PENDING, blankToNull(cmd.note()),
                null, null, null, null));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
