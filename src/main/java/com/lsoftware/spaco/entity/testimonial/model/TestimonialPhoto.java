package com.lsoftware.spaco.entity.testimonial.model;

import com.lsoftware.spaco.entity.clinical.model.PhotoKind;

/** publicFile: the copy in the public store, present only while the testimonial is published. */
public record TestimonialPhoto(Long id, Long sessionPhotoId, PhotoKind kind, int position, String publicFile) {
    public TestimonialPhoto withPublicFile(String file) {
        return new TestimonialPhoto(id, sessionPhotoId, kind, position, file);
    }
}
