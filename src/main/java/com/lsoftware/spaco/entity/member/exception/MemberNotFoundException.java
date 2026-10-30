package com.lsoftware.spaco.entity.member.exception;

import com.lsoftware.spaco.entity.exception.NotFoundException;

public class MemberNotFoundException extends NotFoundException {
    public MemberNotFoundException() {
        super("MEMBER_NOT_FOUND", "Team member not found");
    }
}
