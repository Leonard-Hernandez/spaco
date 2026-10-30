package com.lsoftware.spaco.entity.consent.exception;

import com.lsoftware.spaco.entity.exception.DomainException;

public class ConsentRequiredException extends DomainException {
    public ConsentRequiredException(String what) {
        super("CONSENT_REQUIRED", what);
    }
}
