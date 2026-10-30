package com.lsoftware.spaco.entity.appointment.exception;

import com.lsoftware.spaco.entity.exception.ConflictException;

public class SlotTakenException extends ConflictException {
    public SlotTakenException() {
        super("SLOT_TAKEN", "That time was just taken; pick another one");
    }
}
