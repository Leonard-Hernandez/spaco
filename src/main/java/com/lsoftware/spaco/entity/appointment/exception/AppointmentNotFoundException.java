package com.lsoftware.spaco.entity.appointment.exception;

import com.lsoftware.spaco.entity.exception.NotFoundException;

public class AppointmentNotFoundException extends NotFoundException {
    public AppointmentNotFoundException() {
        super("APPOINTMENT_NOT_FOUND", "Appointment not found");
    }
}
