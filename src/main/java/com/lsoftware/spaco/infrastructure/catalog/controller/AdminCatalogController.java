package com.lsoftware.spaco.infrastructure.catalog.controller;

import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.entity.treatment.model.TreatmentCategory;
import com.lsoftware.spaco.infrastructure.catalog.dto.CategoryRequest;
import com.lsoftware.spaco.infrastructure.catalog.dto.TreatmentRequest;
import com.lsoftware.spaco.infrastructure.catalog.dto.TreatmentResponse;
import com.lsoftware.spaco.usecase.catalog.ManageCatalogUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/panel/admin")
@RequiredArgsConstructor
@Tag(name = "Catalogue (owner)", description = "Categories, treatments and their photos")
@SecurityRequirement(name = "bearerAuth")
public class AdminCatalogController {

    private final ManageCatalogUseCase catalog;
    private final CurrentTenantGateway tenants;

    @GetMapping("/categories")
    public List<TreatmentCategory> categories() {
        return catalog.categories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public TreatmentCategory createCategory(@Valid @RequestBody CategoryRequest req) {
        return catalog.saveCategory(null, req.name(), req.ink(), req.position(), req.active());
    }

    @PutMapping("/categories/{id}")
    public TreatmentCategory updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return catalog.saveCategory(id, req.name(), req.ink(), req.position(), req.active());
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        catalog.deleteCategory(id);
    }

    @GetMapping("/treatments")
    public List<TreatmentResponse> treatments() {
        String slug = tenants.current().slug();
        return catalog.treatments().stream().map(t -> TreatmentResponse.from(t, slug)).toList();
    }

    @PostMapping("/treatments")
    @ResponseStatus(HttpStatus.CREATED)
    public TreatmentResponse createTreatment(@Valid @RequestBody TreatmentRequest req) {
        return TreatmentResponse.from(catalog.saveTreatment(null, req.toCommand()), tenants.current().slug());
    }

    @PutMapping("/treatments/{id}")
    public TreatmentResponse updateTreatment(@PathVariable Long id, @Valid @RequestBody TreatmentRequest req) {
        return TreatmentResponse.from(catalog.saveTreatment(id, req.toCommand()), tenants.current().slug());
    }

    @PutMapping(value = "/treatments/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TreatmentResponse setImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        return TreatmentResponse.from(catalog.setImage(id, file.getBytes()), tenants.current().slug());
    }

    @DeleteMapping("/treatments/{id}/image")
    public TreatmentResponse removeImage(@PathVariable Long id) {
        return TreatmentResponse.from(catalog.removeImage(id), tenants.current().slug());
    }
}
