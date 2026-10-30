package com.lsoftware.spaco.infrastructure.availability.controller;

import com.lsoftware.spaco.entity.availability.model.AvailabilityException;
import com.lsoftware.spaco.entity.availability.model.AvailabilityRule;
import com.lsoftware.spaco.infrastructure.availability.dto.ExceptionRequest;
import com.lsoftware.spaco.infrastructure.availability.dto.RuleRequest;
import com.lsoftware.spaco.usecase.availability.ManageScheduleUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/panel/admin/schedule")
@RequiredArgsConstructor
@Tag(name = "Schedule (owner)", description = "Weekly hours and exceptions")
@SecurityRequirement(name = "bearerAuth")
public class AdminScheduleController {

    private final ManageScheduleUseCase schedule;

    @GetMapping("/rules")
    public List<AvailabilityRule> rules() {
        return schedule.rules();
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityRule createRule(@Valid @RequestBody RuleRequest req) {
        return schedule.saveRule(null, req.professionalId(), req.weekday(), req.startTime(), req.endTime(), req.active());
    }

    @PutMapping("/rules/{id}")
    public AvailabilityRule updateRule(@PathVariable Long id, @Valid @RequestBody RuleRequest req) {
        return schedule.saveRule(id, req.professionalId(), req.weekday(), req.startTime(), req.endTime(), req.active());
    }

    @DeleteMapping("/rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable Long id) {
        schedule.deleteRule(id);
    }

    @GetMapping("/exceptions")
    public List<AvailabilityException> exceptions(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return schedule.exceptions(from, to);
    }

    @PostMapping("/exceptions")
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityException addException(@Valid @RequestBody ExceptionRequest req) {
        return schedule.addException(req.professionalId(), req.date(), req.startTime(), req.endTime(), req.type(), req.note());
    }

    @DeleteMapping("/exceptions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteException(@PathVariable Long id) {
        schedule.deleteException(id);
    }
}
