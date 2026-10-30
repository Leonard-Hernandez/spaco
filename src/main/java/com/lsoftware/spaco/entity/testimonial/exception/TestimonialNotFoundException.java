package com.lsoftware.spaco.entity.testimonial.exception;

import com.lsoftware.spaco.entity.exception.NotFoundException;

public class TestimonialNotFoundException extends NotFoundException {
    public TestimonialNotFoundException() {
        super("TESTIMONIAL_NOT_FOUND", "Testimonial not found");
    }
}
