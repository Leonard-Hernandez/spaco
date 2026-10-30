package com.lsoftware.spaco.entity.professional.exception;

import com.lsoftware.spaco.entity.exception.NotFoundException;

public class ProfessionalNotFoundException extends NotFoundException {
    public ProfessionalNotFoundException() {
        super("PROFESSIONAL_NOT_FOUND", "Professional not found");
    }
}
