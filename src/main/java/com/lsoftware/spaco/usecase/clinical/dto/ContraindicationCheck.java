package com.lsoftware.spaco.usecase.clinical.dto;

import java.util.List;

/**
 * Treatment contraindications next to the patient's history. matches are words of the
 * contraindications found in her record; it is a reminder, the professional decides.
 */
public record ContraindicationCheck(
        Long patientId,
        String patientName,
        Long treatmentId,
        String treatmentName,
        String contraindications,
        List<String> matches,
        List<String> alerts,
        boolean recordFilled,
        boolean treatmentConsentSigned
) {}
