package com.lsoftware.spaco.infrastructure.appointment.dto;

import jakarta.validation.constraints.Size;

public record ReasonRequest(@Size(max = 255) String reason) {}
