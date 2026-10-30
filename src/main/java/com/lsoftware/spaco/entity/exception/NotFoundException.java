package com.lsoftware.spaco.entity.exception;

/** Something the request names does not exist in the current spa (404). */
public class NotFoundException extends DomainException {
    public NotFoundException(String code, String message) {
        super(code, message);
    }
}
