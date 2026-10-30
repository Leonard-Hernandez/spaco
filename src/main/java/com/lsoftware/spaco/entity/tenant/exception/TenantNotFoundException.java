package com.lsoftware.spaco.entity.tenant.exception;

import com.lsoftware.spaco.entity.exception.NotFoundException;

public class TenantNotFoundException extends NotFoundException {
    public TenantNotFoundException() {
        super("TENANT_NOT_FOUND", "Spa not found");
    }
}
