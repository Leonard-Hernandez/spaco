package com.lsoftware.spaco.infrastructure.patient.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientRequest(@NotBlank @Size(max = 120) String fullName, @NotBlank @Size(max = 30) String phone,
                             @Email @Size(max = 150) String email, @Past LocalDate birthDate) {}
