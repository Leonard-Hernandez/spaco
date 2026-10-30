package com.lsoftware.spaco.entity.appointment.model;

public enum AppointmentStatus {
    /** Requested on the website, waiting for the spa. Already holds the slot. */
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    /** The patient came and was treated (a session note can be attached). */
    COMPLETED,
    NO_SHOW;

    public boolean holdsSlot() {
        return this == PENDING || this == ACCEPTED;
    }
}
