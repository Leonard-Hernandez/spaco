package com.lsoftware.spaco.infrastructure.clinical.dto;

import com.lsoftware.spaco.usecase.testimonial.dto.TestimonialCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TestimonialRequest(
        @NotNull Long patientId,
        @NotBlank @Size(max = 80) String displayName,
        @Size(max = 600) String quote,
        Long treatmentId,
        @Size(max = 8) List<@NotNull Long> photoIds
) {
    public TestimonialCommand toCommand() {
        return new TestimonialCommand(patientId, displayName, quote, treatmentId, photoIds);
    }
}
