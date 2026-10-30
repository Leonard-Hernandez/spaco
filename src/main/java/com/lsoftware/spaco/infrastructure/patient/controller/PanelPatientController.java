package com.lsoftware.spaco.infrastructure.patient.controller;

import com.lsoftware.spaco.infrastructure.patient.dto.PatientRequest;
import com.lsoftware.spaco.infrastructure.patient.dto.PatientResponse;
import com.lsoftware.spaco.usecase.patient.ManagePatientUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/panel/patients")
@RequiredArgsConstructor
@Tag(name = "Patients (staff)", description = "Contact data; the clinical record is under /api/panel/clinical")
@SecurityRequirement(name = "bearerAuth")
public class PanelPatientController {

    private final ManagePatientUseCase patients;

    @GetMapping
    @Operation(summary = "Search by name (accents and typos ok), phone or email; empty = most recent")
    public List<PatientResponse> search(@RequestParam(required = false) String q) {
        return patients.search(q).stream().map(PatientResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PatientResponse get(@PathVariable Long id) {
        return PatientResponse.from(patients.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse create(@Valid @RequestBody PatientRequest req) {
        return PatientResponse.from(patients.create(req.fullName(), req.phone(), req.email(), req.birthDate()));
    }

    @PutMapping("/{id}")
    public PatientResponse update(@PathVariable Long id, @Valid @RequestBody PatientRequest req) {
        return PatientResponse.from(patients.update(id, req.fullName(), req.phone(), req.email(), req.birthDate()));
    }
}
