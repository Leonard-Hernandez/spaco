package com.lsoftware.spaco.entity.tenant.gateway;

import com.lsoftware.spaco.entity.tenant.model.Tenant;

import java.util.Map;
import java.util.Optional;

public interface TenantGateway {
    Optional<Tenant> findBySlug(String slug);
    Optional<Tenant> findById(Long id);

    /** Replaces the editable public profile of a spa (phone, address and branding). */
    Tenant updateProfile(Long id, String contactPhone, String address, Map<String, Object> branding);
}
