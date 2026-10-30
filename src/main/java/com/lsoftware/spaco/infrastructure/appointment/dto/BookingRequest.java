package com.lsoftware.spaco.infrastructure.appointment.dto;

import com.lsoftware.spaco.usecase.appointment.dto.BookingCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/** website: honeypot field, hidden from people; bots fill it in. */
public record BookingRequest(
        @NotNull Long treatmentId,
        Long professionalId,
        @NotNull OffsetDateTime startAt,
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Size(max = 30) String phone,
        @Email @Size(max = 150) String email,
        @Size(max = 500) String note,
        boolean acceptsDataPolicy,
        String website
) {
    public BookingCommand toCommand() {
        return new BookingCommand(treatmentId, professionalId, startAt, fullName, phone, email, note, acceptsDataPolicy);
    }
}
