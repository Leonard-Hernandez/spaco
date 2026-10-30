package com.lsoftware.spaco.entity.treatment.exception;

import com.lsoftware.spaco.entity.exception.ConflictException;

public class CategoryNotEmptyException extends ConflictException {
    public CategoryNotEmptyException() {
        super("CATEGORY_NOT_EMPTY", "Move or delete its treatments first");
    }
}
