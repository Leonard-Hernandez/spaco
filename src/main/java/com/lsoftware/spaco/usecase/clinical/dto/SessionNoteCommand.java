package com.lsoftware.spaco.usecase.clinical.dto;

import java.time.OffsetDateTime;

/** appointmentId set = the note of that visit (it completes the appointment). */
public record SessionNoteCommand(
        Long appointmentId,
        Long treatmentId,
        Long professionalId,
        OffsetDateTime performedAt,
        String products,
        String parameters,
        String observations,
        String recommendations
) {}
