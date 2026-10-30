package com.lsoftware.spaco.entity.clinical.exception;

import com.lsoftware.spaco.entity.exception.NotFoundException;

public class SessionNoteNotFoundException extends NotFoundException {
    public SessionNoteNotFoundException() {
        super("SESSION_NOT_FOUND", "Session note not found");
    }
}
