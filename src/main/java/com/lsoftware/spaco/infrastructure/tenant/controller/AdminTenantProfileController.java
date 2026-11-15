package com.lsoftware.spaco.infrastructure.tenant.controller;

import com.lsoftware.spaco.infrastructure.tenant.dto.TenantProfileRequest;
import com.lsoftware.spaco.infrastructure.tenant.dto.TenantProfileResponse;
import com.lsoftware.spaco.usecase.tenant.ManageTenantProfileUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/panel/admin/profile")
@RequiredArgsConstructor
@Tag(name = "Spa profile (owner)", description = "Contact details and texts shown on the public site")
@SecurityRequirement(name = "bearerAuth")
public class AdminTenantProfileController {

    private final ManageTenantProfileUseCase profile;

    @GetMapping
    public TenantProfileResponse get() {
        return TenantProfileResponse.from(profile.get());
    }

    @PutMapping
    public TenantProfileResponse update(@Valid @RequestBody TenantProfileRequest req) {
        return TenantProfileResponse.from(profile.update(req.toCommand()));
    }
}
