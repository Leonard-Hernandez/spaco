package com.lsoftware.spaco.entity.clinical.exception;

import com.lsoftware.spaco.entity.exception.NotFoundException;

public class PhotoNotFoundException extends NotFoundException {
    public PhotoNotFoundException() {
        super("PHOTO_NOT_FOUND", "Photo not found");
    }
}
