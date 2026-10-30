package com.lsoftware.spaco.infrastructure.catalog.controller;

import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.infrastructure.catalog.dto.CatalogResponse;
import com.lsoftware.spaco.infrastructure.catalog.dto.TreatmentResponse;
import com.lsoftware.spaco.usecase.catalog.GetCatalogUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Catalogue (public)", description = "Treatments the spa offers")
public class CatalogController {

    private final GetCatalogUseCase catalog;
    private final CurrentTenantGateway tenants;

    @GetMapping("/catalog")
    @Operation(summary = "Active categories and treatments")
    public CatalogResponse catalog() {
        var result = catalog.execute();
        String slug = tenants.current().slug();
        return new CatalogResponse(result.categories(),
                result.treatments().stream().map(t -> TreatmentResponse.from(t, slug)).toList());
    }

    @GetMapping("/treatments/{slug}")
    @Operation(summary = "One treatment by its URL name")
    public TreatmentResponse treatment(@PathVariable String slug) {
        return TreatmentResponse.from(catalog.bySlug(slug), tenants.current().slug());
    }
}
