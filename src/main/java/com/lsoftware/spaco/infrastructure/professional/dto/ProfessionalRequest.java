package com.lsoftware.spaco.infrastructure.professional.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfessionalRequest(@NotBlank @Size(max = 120) String displayName, @Size(max = 120) String title,
                                  @Size(max = 2000) String bio, Long memberId, boolean active) {}
