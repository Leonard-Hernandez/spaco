package com.lsoftware.spaco.entity.treatment.exception;

import com.lsoftware.spaco.entity.exception.NotFoundException;

public class TreatmentNotFoundException extends NotFoundException {
    public TreatmentNotFoundException() {
        super("TREATMENT_NOT_FOUND", "Treatment not found");
    }
}
