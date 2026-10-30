package com.lsoftware.spaco.usecase.patient;

import com.lsoftware.spaco.entity.patient.gateway.PatientGateway;
import com.lsoftware.spaco.entity.patient.model.Patient;
import com.lsoftware.spaco.entity.patient.model.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Finds the patient behind a booking by phone, or registers her. No account is needed to book;
 * a known patient keeps her record, and a missing email is filled in.
 */
@Component
@RequiredArgsConstructor
public class ProvisionPatientUseCase {

    private final PatientGateway patients;

    public Patient execute(String fullName, String rawPhone, String email) {
        String phone = PhoneNumber.toE164(rawPhone);
        String mail = email == null || email.isBlank() ? null : email.strip().toLowerCase(Locale.ROOT);
        return patients.findByPhone(phone)
                .map(p -> p.email() == null && mail != null
                        ? patients.save(p.withContact(p.fullName(), p.phoneE164(), mail, p.birthDate()))
                        : p)
                .orElseGet(() -> patients.save(new Patient(null, fullName.strip(), phone, mail, null, null, null)));
    }
}
