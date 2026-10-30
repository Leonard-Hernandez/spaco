package com.lsoftware.spaco.entity.tenant.gateway;

import com.lsoftware.spaco.entity.tenant.model.Tenant;

import java.util.Optional;

public interface TenantGateway {
    Optional<Tenant> findBySlug(String slug);
    Optional<Tenant> findById(Long id);
}
