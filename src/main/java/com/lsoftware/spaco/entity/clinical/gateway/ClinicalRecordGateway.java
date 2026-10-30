package com.lsoftware.spaco.entity.clinical.gateway;

import com.lsoftware.spaco.entity.clinical.model.ClinicalRecord;

import java.util.Optional;

public interface ClinicalRecordGateway {
    ClinicalRecord save(ClinicalRecord record);
    Optional<ClinicalRecord> findByPatient(Long patientId);
}
