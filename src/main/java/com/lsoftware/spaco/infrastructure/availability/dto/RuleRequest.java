package com.lsoftware.spaco.infrastructure.availability.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record RuleRequest(@NotNull Long professionalId, @NotNull DayOfWeek weekday, @NotNull LocalTime startTime,
                          @NotNull LocalTime endTime, boolean active) {}
