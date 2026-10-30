package com.lsoftware.spaco.infrastructure.availability.controller;

import com.lsoftware.spaco.usecase.availability.GetAvailableSlotsUseCase;
import com.lsoftware.spaco.usecase.availability.dto.SlotsResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Tag(name = "Availability (public)", description = "Free start times")
public class AvailabilityController {

    private final GetAvailableSlotsUseCase slots;

    @GetMapping("/api/public/availability")
    @Operation(summary = "Free start times (spa local time) for a treatment on a date; no professional = any")
    public SlotsResult availability(@RequestParam Long treatmentId,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                    @RequestParam(required = false) Long professionalId) {
        return slots.execute(treatmentId, professionalId, date);
    }
}
