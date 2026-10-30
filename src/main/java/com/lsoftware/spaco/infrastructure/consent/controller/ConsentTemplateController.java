package com.lsoftware.spaco.infrastructure.consent.controller;

import com.lsoftware.spaco.entity.consent.model.ConsentTemplate;
import com.lsoftware.spaco.entity.consent.model.ConsentType;
import com.lsoftware.spaco.infrastructure.consent.dto.TemplateRequest;
import com.lsoftware.spaco.usecase.consent.ConsentUseCase;
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
@Tag(name = "Consent texts", description = "What patients accept, versioned")
public class ConsentTemplateController {

    private final ConsentUseCase consents;

    @GetMapping("/api/public/consents/{type}")
    @Operation(summary = "Current text of a consent (shown before accepting it)")
    public ConsentTemplate current(@PathVariable ConsentType type) {
        return consents.currentTemplate(type);
    }

    @GetMapping("/api/panel/admin/consent-templates")
    @SecurityRequirement(name = "bearerAuth")
    public List<ConsentTemplate> all() {
        return consents.templates();
    }

    @PostMapping("/api/panel/admin/consent-templates")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Publish a new version of a text")
    public ConsentTemplate publish(@Valid @RequestBody TemplateRequest req) {
        return consents.publishTemplate(req.type(), req.title(), req.body());
    }
}
