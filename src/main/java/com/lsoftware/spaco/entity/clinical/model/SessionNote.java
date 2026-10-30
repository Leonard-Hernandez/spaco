package com.lsoftware.spaco.entity.clinical.model;

import java.time.OffsetDateTime;
import java.util.List;

/** What was done in one visit: products, device parameters, observations, home care. */
public record SessionNote(
        Long id,
        Long patientId,
        Long appointmentId,
        Long treatmentId,
        String treatmentName,
        Long professionalId,
        String professionalName,
        OffsetDateTime performedAt,
        String products,
        String parameters,
        String observations,
        String recommendations,
        String createdBy,
        List<SessionPhoto> photos
) {}
