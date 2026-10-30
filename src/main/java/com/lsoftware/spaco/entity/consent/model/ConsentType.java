package com.lsoftware.spaco.entity.consent.model;

public enum ConsentType {
    /** Personal and health data processing (Ley 1581 de 2012). Required to book. */
    DATA_PROCESSING,
    /** Informed consent for one treatment, signed at the spa. */
    TREATMENT,
    /** Publishing before/after photos and a testimonial on the website. Revocable any time. */
    PHOTO_TESTIMONIAL
}
