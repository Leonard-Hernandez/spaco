package com.lsoftware.spaco.entity.testimonial.model;

import java.time.OffsetDateTime;
import java.util.List;

public record Testimonial(
        Long id,
        Long patientId,
        String displayName,
        String quote,
        Long treatmentId,
        String treatmentName,
        TestimonialStatus status,
        OffsetDateTime publishedAt,
        List<TestimonialPhoto> photos
) {
    public Testimonial with(TestimonialStatus newStatus, OffsetDateTime at, List<TestimonialPhoto> newPhotos) {
        return new Testimonial(id, patientId, displayName, quote, treatmentId, treatmentName, newStatus, at, newPhotos);
    }

    public Testimonial withText(String name, String text, Long treatment, List<TestimonialPhoto> newPhotos) {
        return new Testimonial(id, patientId, name, text, treatment, treatmentName, status, publishedAt, newPhotos);
    }
}
