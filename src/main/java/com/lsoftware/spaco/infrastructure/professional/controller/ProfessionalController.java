package com.lsoftware.spaco.infrastructure.professional.controller;

import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.infrastructure.professional.dto.ProfessionalRequest;
import com.lsoftware.spaco.infrastructure.professional.dto.ProfessionalResponse;
import com.lsoftware.spaco.usecase.professional.ManageProfessionalUseCase;
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
@RequiredArgsConstructor
@Tag(name = "Professionals", description = "Who performs the treatments")
public class ProfessionalController {

    private final ManageProfessionalUseCase professionals;
    private final CurrentTenantGateway tenants;

    @GetMapping("/api/public/professionals")
    public List<ProfessionalResponse> active() {
        String slug = tenants.current().slug();
        return professionals.listActive().stream().map(p -> ProfessionalResponse.from(p, slug)).toList();
    }

    @GetMapping("/api/panel/professionals")
    @SecurityRequirement(name = "bearerAuth")
    public List<ProfessionalResponse> all() {
        String slug = tenants.current().slug();
        return professionals.list().stream().map(p -> ProfessionalResponse.from(p, slug)).toList();
    }

    @PostMapping("/api/panel/admin/professionals")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    public ProfessionalResponse create(@Valid @RequestBody ProfessionalRequest req) {
        return ProfessionalResponse.from(professionals.save(null, req.displayName(), req.title(), req.bio(),
                req.memberId(), req.active()), tenants.current().slug());
    }

    @PutMapping("/api/panel/admin/professionals/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ProfessionalResponse update(@PathVariable Long id, @Valid @RequestBody ProfessionalRequest req) {
        return ProfessionalResponse.from(professionals.save(id, req.displayName(), req.title(), req.bio(),
                req.memberId(), req.active()), tenants.current().slug());
    }

    @PutMapping(value = "/api/panel/admin/professionals/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    public ProfessionalResponse photo(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        return ProfessionalResponse.from(professionals.setPhoto(id, file.getBytes()), tenants.current().slug());
    }
}
