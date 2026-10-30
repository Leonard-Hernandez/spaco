package com.lsoftware.spaco.infrastructure.config.tenant;

import com.lsoftware.spaco.entity.tenant.exception.TenantNotFoundException;
import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.entity.tenant.model.Tenant;
import org.springframework.stereotype.Component;

@Component
public class CurrentTenantContextGateway implements CurrentTenantGateway {

    @Override
    public Tenant current() {
        return TenantContext.current().orElseThrow(TenantNotFoundException::new);
    }
}
