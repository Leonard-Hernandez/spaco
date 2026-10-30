package com.lsoftware.spaco.entity.availability.exception;

import com.lsoftware.spaco.entity.exception.NotFoundException;

public class RuleNotFoundException extends NotFoundException {
    public RuleNotFoundException() {
        super("RULE_NOT_FOUND", "Schedule rule not found");
    }
}
