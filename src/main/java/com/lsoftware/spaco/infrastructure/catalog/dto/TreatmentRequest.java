package com.lsoftware.spaco.infrastructure.catalog.dto;

import com.lsoftware.spaco.entity.treatment.model.CompositionLine;
import com.lsoftware.spaco.usecase.catalog.dto.TreatmentCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TreatmentRequest(
        @NotNull Long categoryId,
        Integer formulaNo,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 200) String tagline,
        @Size(max = 4000) String description,
        @Size(max = 12) List<@NotBlank @Size(max = 160) String> benefits,
        @Size(max = 12) List<CompositionLine> composition,
        @Size(max = 4000) String contraindications,
        @Min(5) @Max(600) int durationMin,
        @Min(0) @Max(120) int bufferMin,
        @Min(0) Long price,
        int position,
        boolean active
) {
    public TreatmentCommand toCommand() {
        return new TreatmentCommand(categoryId, formulaNo, name, tagline, description, benefits,
                composition == null ? null : composition.stream()
                        .filter(c -> c.label() != null && !c.label().isBlank() && c.value() != null)
                        .toList(),
                contraindications, durationMin, bufferMin, price, position, active);
    }
}
