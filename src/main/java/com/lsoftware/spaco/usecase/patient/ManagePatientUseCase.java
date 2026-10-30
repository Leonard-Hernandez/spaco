package com.lsoftware.spaco.usecase.patient;

import com.lsoftware.spaco.entity.patient.exception.PatientNotFoundException;
import com.lsoftware.spaco.entity.patient.gateway.PatientGateway;
import com.lsoftware.spaco.entity.patient.model.Patient;
import com.lsoftware.spaco.entity.patient.model.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/** Patient contact data, for every staff role (clinical data is elsewhere). */
@Component
@RequiredArgsConstructor
public class ManagePatientUseCase {

    static final int MAX_RESULTS = 30;

    private final PatientGateway patients;

    @Transactional(readOnly = true)
    public List<Patient> search(String query) {
        return patients.search(query == null ? "" : query.strip(), MAX_RESULTS);
    }

    @Transactional(readOnly = true)
    public Patient get(Long id) {
        return patients.findById(id).orElseThrow(PatientNotFoundException::new);
    }

    @Transactional
    public Patient create(String fullName, String phone, String email, LocalDate birthDate) {
        return patients.save(new Patient(null, fullName.strip(), PhoneNumber.toE164(phone), clean(email), birthDate,
                null, null));
    }

    /** Changing the email unlinks nothing: an already linked account stays linked. */
    @Transactional
    public Patient update(Long id, String fullName, String phone, String email, LocalDate birthDate) {
        Patient patient = get(id);
        return patients.save(patient.withContact(fullName.strip(), PhoneNumber.toE164(phone), clean(email), birthDate));
    }

    private static String clean(String email) {
        return email == null || email.isBlank() ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
