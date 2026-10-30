package com.lsoftware.spaco.entity.consent.exception;

import com.lsoftware.spaco.entity.exception.DomainException;

public class ConsentTemplateMissingException extends DomainException {
    public ConsentTemplateMissingException() {
        super("CONSENT_TEMPLATE_MISSING", "The spa has not published that consent text yet");
    }
}
