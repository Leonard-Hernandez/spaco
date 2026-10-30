package com.lsoftware.spaco.usecase.appointment.dto;

import java.time.OffsetDateTime;

/** A booking from the website. professionalId null = no preference. */
public record BookingCommand(
        Long treatmentId,
        Long professionalId,
        OffsetDateTime startAt,
        String fullName,
        String phone,
        String email,
        String note,
        boolean acceptsDataPolicy
) {}
