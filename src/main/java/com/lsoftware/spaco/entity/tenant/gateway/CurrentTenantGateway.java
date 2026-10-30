package com.lsoftware.spaco.entity.tenant.gateway;

import com.lsoftware.spaco.entity.tenant.model.Tenant;

/**
 * Spa the current request belongs to (resolved from the X-Tenant header).
 */
public interface CurrentTenantGateway {
    Tenant current();
}
