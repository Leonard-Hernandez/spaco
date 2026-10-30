package com.lsoftware.spaco.entity.patient.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Someone who booked at the spa. Created from the booking form, without an account; authcoSub
 * is set when she opens her clinical history with a Google account whose verified email matches.
 */
public record Patient(
        Long id,
        String fullName,
        String phoneE164,
        String email,
        LocalDate birthDate,
        String authcoSub,
        OffsetDateTime createdAt
) {
    public Patient linkedTo(String sub) {
        return new Patient(id, fullName, phoneE164, email, birthDate, sub, createdAt);
    }

    public Patient withContact(String name, String phone, String mail, LocalDate birth) {
        return new Patient(id, name, phone, mail, birth, authcoSub, createdAt);
    }
}
