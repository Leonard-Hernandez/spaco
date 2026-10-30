package com.lsoftware.spaco.infrastructure.testimonial.controller;

import com.lsoftware.spaco.entity.testimonial.model.Testimonial;
import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.infrastructure.clinical.dto.TestimonialRequest;
import com.lsoftware.spaco.infrastructure.testimonial.dto.PublicTestimonialResponse;
import com.lsoftware.spaco.usecase.testimonial.TestimonialUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Testimonials", description = "Before/after stories the patients authorized")
public class TestimonialController {

    private final TestimonialUseCase testimonials;
    private final CurrentTenantGateway tenants;

    @GetMapping("/api/public/testimonials")
    @Operation(summary = "Published testimonials (patient consent still valid)")
    public List<PublicTestimonialResponse> published() {
        String slug = tenants.current().slug();
        return testimonials.published().stream().map(t -> PublicTestimonialResponse.from(t, slug)).toList();
    }

    @GetMapping("/api/panel/clinical/testimonials")
    @SecurityRequirement(name = "bearerAuth")
    public List<Testimonial> all() {
        return testimonials.list();
    }

    @PostMapping("/api/panel/clinical/testimonials")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public Testimonial create(@Valid @RequestBody TestimonialRequest req) {
        return testimonials.create(req.toCommand());
    }

    @PutMapping("/api/panel/clinical/testimonials/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public Testimonial update(@PathVariable Long id, @Valid @RequestBody TestimonialRequest req) {
        return testimonials.update(id, req.toCommand());
    }

    @PostMapping("/api/panel/clinical/testimonials/{id}/publish")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Publish (requires the patient's photo consent)")
    public Testimonial publish(@PathVariable Long id) {
        return testimonials.publish(id);
    }

    @PostMapping("/api/panel/clinical/testimonials/{id}/hide")
    @SecurityRequirement(name = "bearerAuth")
    public Testimonial hide(@PathVariable Long id) {
        return testimonials.hide(id);
    }

    @DeleteMapping("/api/panel/clinical/testimonials/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    public void delete(@PathVariable Long id) {
        testimonials.delete(id);
    }
}
