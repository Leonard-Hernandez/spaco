package com.lsoftware.spaco.infrastructure.availability.dto;

import com.lsoftware.spaco.entity.availability.model.ExceptionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/** professionalId null = the whole spa; no times = the whole day (BLOCK only). */
public record ExceptionRequest(Long professionalId, @NotNull LocalDate date, LocalTime startTime, LocalTime endTime,
                               @NotNull ExceptionType type, @Size(max = 255) String note) {}
