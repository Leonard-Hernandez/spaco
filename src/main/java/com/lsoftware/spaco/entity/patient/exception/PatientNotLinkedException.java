package com.lsoftware.spaco.entity.patient.exception;

import com.lsoftware.spaco.entity.exception.NotFoundException;

/** The signed-in account matches no patient of this spa (different or unverified email). */
public class PatientNotLinkedException extends NotFoundException {
    public PatientNotLinkedException() {
        super("PATIENT_NOT_LINKED", "No patient of this spa uses the email of this account");
    }
}
