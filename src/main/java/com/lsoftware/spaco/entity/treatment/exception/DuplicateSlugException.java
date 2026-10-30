package com.lsoftware.spaco.entity.treatment.exception;

import com.lsoftware.spaco.entity.exception.ConflictException;

public class DuplicateSlugException extends ConflictException {
    public DuplicateSlugException() {
        super("DUPLICATE_NAME", "Another one already has that name");
    }
}
