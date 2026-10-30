package com.lsoftware.spaco.entity.patient.model;

import com.lsoftware.spaco.entity.exception.DomainException;

/**
 * Normalizes patient phones to E.164. Bare 10-digit Colombian mobiles (3xx...) get +57.
 */
public final class PhoneNumber {

    private static final String DEFAULT_COUNTRY_CODE = "57";

    private PhoneNumber() {}

    public static String toE164(String raw) {
        if (raw == null) throw invalid();
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 10 && digits.startsWith("3")) {
            digits = DEFAULT_COUNTRY_CODE + digits;
        }
        if (digits.length() < 10 || digits.length() > 15) throw invalid();
        return "+" + digits;
    }

    /** Digits only, the format wa.me links expect. */
    public static String digits(String e164) {
        return e164.replaceAll("\\D", "");
    }

    private static DomainException invalid() {
        return new DomainException("INVALID_PHONE", "Phone number is not valid");
    }
}
