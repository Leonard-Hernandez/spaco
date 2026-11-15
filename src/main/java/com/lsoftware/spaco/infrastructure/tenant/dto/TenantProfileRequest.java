package com.lsoftware.spaco.infrastructure.tenant.dto;

import com.lsoftware.spaco.usecase.tenant.dto.TenantProfileCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TenantProfileRequest(
        @Size(max = 30) @Pattern(regexp = "^[+\\d\\s().-]*$", message = "digits only") String phone,
        @Size(max = 120) @Email String email,
        @Size(max = 255) String address,
        @Size(max = 80) String city,
        @Size(max = 160) String tagline,
        @Size(max = 600) String intro,
        @Size(max = 60) @Pattern(regexp = "^@?[A-Za-z0-9._]*$", message = "not a valid Instagram handle") String instagram,
        @Size(max = 300) String cancellationNote
) {
    public TenantProfileCommand toCommand() {
        return new TenantProfileCommand(phone, email, address, city, tagline, intro, instagram, cancellationNote);
    }
}
