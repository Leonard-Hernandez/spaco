package com.lsoftware.spaco.entity.appointment.exception;

import com.lsoftware.spaco.entity.exception.ConflictException;

public class InvalidAppointmentStateException extends ConflictException {
    public InvalidAppointmentStateException() {
        super("INVALID_APPOINTMENT_STATE", "The appointment cannot change to that state");
    }
}
