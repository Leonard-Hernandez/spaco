package com.lsoftware.spaco.usecase.testimonial.dto;

import java.util.List;

/** photoIds: clinical photos of that patient, in carousel order. */
public record TestimonialCommand(Long patientId, String displayName, String quote, Long treatmentId,
                                 List<Long> photoIds) {}
