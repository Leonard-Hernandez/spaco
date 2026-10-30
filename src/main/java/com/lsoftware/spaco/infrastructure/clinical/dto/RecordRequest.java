package com.lsoftware.spaco.infrastructure.clinical.dto;

import com.lsoftware.spaco.usecase.clinical.dto.RecordCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record RecordRequest(
        @Size(max = 4000) String allergies,
        @Size(max = 4000) String medications,
        @Size(max = 4000) String conditions,
        @Size(max = 120) String pregnancy,
        @Size(max = 60) String skinType,
        @Min(1) @Max(6) Integer phototype,
        @Size(max = 4000) String surgeries,
        @Size(max = 4000) String habits,
        @Size(max = 8000) String observations
) {
    public RecordCommand toCommand() {
        return new RecordCommand(allergies, medications, conditions, pregnancy, skinType, phototype, surgeries,
                habits, observations);
    }
}
