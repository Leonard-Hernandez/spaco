package com.lsoftware.spaco.infrastructure.testimonial.dto;

import com.lsoftware.spaco.entity.clinical.model.PhotoKind;
import com.lsoftware.spaco.entity.testimonial.model.Testimonial;

import java.time.OffsetDateTime;
import java.util.List;

/** No patient id or name: only what she authorized to show. */
public record PublicTestimonialResponse(Long id, String displayName, String quote, String treatmentName,
                                        OffsetDateTime publishedAt, List<Photo> photos) {

    public record Photo(PhotoKind kind, String url) {}

    public static PublicTestimonialResponse from(Testimonial t, String tenantSlug) {
        return new PublicTestimonialResponse(t.id(), t.displayName(), t.quote(), t.treatmentName(), t.publishedAt(),
                t.photos().stream()
                        .filter(p -> p.publicFile() != null)
                        .map(p -> new Photo(p.kind(), "/media/" + tenantSlug + "/" + p.publicFile()))
                        .toList());
    }
}
