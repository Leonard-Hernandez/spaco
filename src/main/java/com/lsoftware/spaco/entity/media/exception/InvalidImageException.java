package com.lsoftware.spaco.entity.media.exception;

import com.lsoftware.spaco.entity.exception.DomainException;

public class InvalidImageException extends DomainException {
    public InvalidImageException() {
        super("INVALID_IMAGE", "Upload a JPG, PNG or WebP photo of up to 8 MB");
    }
}
