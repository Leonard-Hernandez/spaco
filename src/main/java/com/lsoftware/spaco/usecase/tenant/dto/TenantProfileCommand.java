package com.lsoftware.spaco.usecase.tenant.dto;

/** Public profile fields the owner edits. Blank or null clears the field. */
public record TenantProfileCommand(
        String phone,
        String email,
        String address,
        String city,
        String tagline,
        String intro,
        String instagram,
        String cancellationNote
) {}
