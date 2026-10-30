package com.lsoftware.spaco.entity.availability.exception;

import com.lsoftware.spaco.entity.exception.NotFoundException;

public class ExceptionNotFoundException extends NotFoundException {
    public ExceptionNotFoundException() {
        super("EXCEPTION_NOT_FOUND", "Schedule exception not found");
    }
}
