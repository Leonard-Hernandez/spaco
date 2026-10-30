package com.lsoftware.spaco.infrastructure.appointment.controller;

import com.lsoftware.spaco.entity.exception.DomainException;
import com.lsoftware.spaco.infrastructure.appointment.dto.BookingRequest;
import com.lsoftware.spaco.infrastructure.appointment.dto.BookingResponse;
import com.lsoftware.spaco.usecase.appointment.BookAppointmentUseCase;
import com.lsoftware.spaco.usecase.appointment.ManageAppointmentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/appointments")
@RequiredArgsConstructor
@Tag(name = "Booking (public)", description = "Book without an account; manage it with its link")
public class BookingController {

    private final BookAppointmentUseCase book;
    private final ManageAppointmentUseCase appointments;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Request an appointment (PENDING until the spa confirms it)")
    public BookingResponse book(@Valid @RequestBody BookingRequest req) {
        if (req.website() != null && !req.website().isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "Malformed request");
        }
        return BookingResponse.from(book.execute(req.toCommand()));
    }

    @GetMapping("/{token}")
    public BookingResponse get(@PathVariable UUID token) {
        return BookingResponse.from(appointments.byToken(token));
    }

    @DeleteMapping("/{token}")
    @Operation(summary = "Cancel from the booking link")
    public BookingResponse cancel(@PathVariable UUID token) {
        return BookingResponse.from(appointments.cancelByToken(token));
    }
}
