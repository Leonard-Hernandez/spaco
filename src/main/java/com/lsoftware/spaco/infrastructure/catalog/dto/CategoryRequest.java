package com.lsoftware.spaco.infrastructure.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Pattern(regexp = "honey|lavender|sage|navy|clay") String ink,
        int position,
        boolean active
) {}
