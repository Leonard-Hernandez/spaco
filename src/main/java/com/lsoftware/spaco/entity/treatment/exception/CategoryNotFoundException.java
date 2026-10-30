package com.lsoftware.spaco.entity.treatment.exception;

import com.lsoftware.spaco.entity.exception.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {
    public CategoryNotFoundException() {
        super("CATEGORY_NOT_FOUND", "Category not found");
    }
}
