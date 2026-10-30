package com.lsoftware.spaco.infrastructure.consent.dto;

import com.lsoftware.spaco.entity.consent.model.ConsentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TemplateRequest(@NotNull ConsentType type, @NotBlank @Size(max = 160) String title,
                              @NotBlank @Size(max = 20000) String body) {}
