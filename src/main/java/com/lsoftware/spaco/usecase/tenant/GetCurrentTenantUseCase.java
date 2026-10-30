package com.lsoftware.spaco.usecase.tenant;

import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.entity.tenant.model.Tenant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetCurrentTenantUseCase {

    private final CurrentTenantGateway tenants;

    public Tenant execute() {
        return tenants.current();
    }
}
