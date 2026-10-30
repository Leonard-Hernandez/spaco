package com.lsoftware.spaco.usecase.availability.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record SlotsResult(LocalDate date, Long treatmentId, Long professionalId, int durationMin, List<LocalTime> slots) {}
