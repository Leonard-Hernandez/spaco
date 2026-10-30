package com.lsoftware.spaco.entity.appointment.exception;

import com.lsoftware.spaco.entity.exception.DomainException;

/** Mapped to 429: a phone number cannot hold more slots than this while the spa answers. */
public class TooManyPendingException extends DomainException {
    public TooManyPendingException() {
        super("TOO_MANY_PENDING", "You already have requests waiting for the spa to answer");
    }
}
