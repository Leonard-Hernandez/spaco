package com.lsoftware.spaco.entity.clinical.model;

import java.time.OffsetDateTime;

/** Anamnesis of a patient, kept up to date by the esthetician. phototype: Fitzpatrick 1-6. */
public record ClinicalRecord(
        Long patientId,
        String allergies,
        String medications,
        String conditions,
        String pregnancy,
        String skinType,
        Integer phototype,
        String surgeries,
        String habits,
        String observations,
        String updatedBy,
        OffsetDateTime updatedAt
) {
    public static ClinicalRecord empty(Long patientId) {
        return new ClinicalRecord(patientId, null, null, null, null, null, null, null, null, null, null, null);
    }

    /** Whether anything was ever written (an empty record is not stored). */
    public boolean filled() {
        return updatedAt != null;
    }
}
