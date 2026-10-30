package com.lsoftware.spaco.entity.testimonial.gateway;

import com.lsoftware.spaco.entity.testimonial.model.Testimonial;
import com.lsoftware.spaco.entity.testimonial.model.TestimonialStatus;

import java.util.List;
import java.util.Optional;

public interface TestimonialGateway {
    Testimonial save(Testimonial testimonial);
    Optional<Testimonial> findById(Long id);
    List<Testimonial> findAll();
    List<Testimonial> findByStatus(TestimonialStatus status);
    List<Testimonial> findByPatient(Long patientId);
    void deleteById(Long id);
}
