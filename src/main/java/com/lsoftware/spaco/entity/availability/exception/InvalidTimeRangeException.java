package com.lsoftware.spaco.entity.availability.exception;

import com.lsoftware.spaco.entity.exception.DomainException;

public class InvalidTimeRangeException extends DomainException {
    public InvalidTimeRangeException() {
        super("INVALID_TIME_RANGE", "The start must be before the end");
    }
}
