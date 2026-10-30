package com.lsoftware.spaco.entity.member.exception;

import com.lsoftware.spaco.entity.exception.DomainException;

/** An owner removing or demoting themselves could lock the spa out of its panel. */
public class SelfChangeException extends DomainException {
    public SelfChangeException() {
        super("SELF_CHANGE", "You cannot remove or demote yourself");
    }
}
