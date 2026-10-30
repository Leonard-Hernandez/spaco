package com.lsoftware.spaco.infrastructure.clinical.dto;

import com.lsoftware.spaco.usecase.clinical.dto.SessionNoteCommand;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record SessionNoteRequest(
        Long appointmentId,
        Long treatmentId,
        Long professionalId,
        OffsetDateTime performedAt,
        @Size(max = 4000) String products,
        @Size(max = 4000) String parameters,
        @Size(max = 8000) String observations,
        @Size(max = 4000) String recommendations
) {
    public SessionNoteCommand toCommand() {
        return new SessionNoteCommand(appointmentId, treatmentId, professionalId, performedAt, products, parameters,
                observations, recommendations);
    }
}
