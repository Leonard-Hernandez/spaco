package com.lsoftware.spaco.entity.availability.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * professionalId null = applies to the whole spa. A BLOCK without times closes the whole day.
 */
public record AvailabilityException(
        Long id,
        Long professionalId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        ExceptionType type,
        String note
) {
    public boolean wholeDay() {
        return startTime == null || endTime == null;
    }
}
