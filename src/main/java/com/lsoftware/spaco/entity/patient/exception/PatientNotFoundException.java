package com.lsoftware.spaco.entity.patient.exception;

import com.lsoftware.spaco.entity.exception.NotFoundException;

public class PatientNotFoundException extends NotFoundException {
    public PatientNotFoundException() {
        super("PATIENT_NOT_FOUND", "Patient not found");
    }
}
