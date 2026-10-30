package com.lsoftware.spaco.entity.patient.gateway;

import com.lsoftware.spaco.entity.patient.model.Patient;

import java.util.List;
import java.util.Optional;

public interface PatientGateway {
    Patient save(Patient patient);
    Optional<Patient> findById(Long id);
    Optional<Patient> findByPhone(String phoneE164);
    Optional<Patient> findBySub(String authcoSub);
    /** Patients with that email (case-insensitive) not linked to any account yet. */
    List<Patient> findUnlinkedByEmail(String email);
    /** By name (fuzzy), phone or email; most recent first. */
    List<Patient> search(String query, int limit);
}
