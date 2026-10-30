package com.lsoftware.spaco.infrastructure.clinical.dto;

import com.lsoftware.spaco.entity.consent.model.ConsentType;
import jakarta.validation.constraints.NotNull;

/** treatmentId only for TREATMENT consents. */
public record ConsentRequest(@NotNull ConsentType type, Long treatmentId) {}
