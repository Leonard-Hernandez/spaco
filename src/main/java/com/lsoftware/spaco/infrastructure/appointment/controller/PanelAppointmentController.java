package com.lsoftware.spaco.infrastructure.appointment.controller;

import com.lsoftware.spaco.entity.appointment.model.AppointmentStatus;
import com.lsoftware.spaco.infrastructure.appointment.dto.AppointmentResponse;
import com.lsoftware.spaco.infrastructure.appointment.dto.ReasonRequest;
import com.lsoftware.spaco.usecase.appointment.ManageAppointmentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/panel/appointments")
@RequiredArgsConstructor
@Tag(name = "Agenda (staff)", description = "Requests and appointments")
@SecurityRequirement(name = "bearerAuth")
public class PanelAppointmentController {

    private final ManageAppointmentUseCase appointments;

    @GetMapping
    @Operation(summary = "Appointments starting in [from, to)")
    public List<AppointmentResponse> list(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
                                          @RequestParam(required = false) AppointmentStatus status,
                                          @RequestParam(required = false) Long professionalId) {
        return appointments.list(from, to, status, professionalId).stream().map(AppointmentResponse::from).toList();
    }

    @GetMapping("/pending")
    @Operation(summary = "Requests waiting for an answer")
    public List<AppointmentResponse> pending() {
        return appointments.pending().stream().map(AppointmentResponse::from).toList();
    }

    @GetMapping("/patient/{patientId}")
    public List<AppointmentResponse> ofPatient(@PathVariable Long patientId) {
        return appointments.ofPatient(patientId).stream().map(AppointmentResponse::from).toList();
    }

    @PostMapping("/{id}/accept")
    public AppointmentResponse accept(@PathVariable Long id) {
        return AppointmentResponse.from(appointments.accept(id));
    }

    @PostMapping("/{id}/reject")
    public AppointmentResponse reject(@PathVariable Long id, @Valid @RequestBody(required = false) ReasonRequest req) {
        return AppointmentResponse.from(appointments.reject(id, req == null ? null : req.reason()));
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id, @Valid @RequestBody(required = false) ReasonRequest req) {
        return AppointmentResponse.from(appointments.cancel(id, req == null ? null : req.reason()));
    }

    @PostMapping("/{id}/complete")
    public AppointmentResponse complete(@PathVariable Long id) {
        return AppointmentResponse.from(appointments.complete(id));
    }

    @PostMapping("/{id}/no-show")
    public AppointmentResponse noShow(@PathVariable Long id) {
        return AppointmentResponse.from(appointments.noShow(id));
    }
}
