package com.lsoftware.spaco.entity.availability.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityRule(
        Long id,
        Long professionalId,
        DayOfWeek weekday,
        LocalTime startTime,
        LocalTime endTime,
        boolean active
) {}
