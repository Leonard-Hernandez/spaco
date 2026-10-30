package com.lsoftware.spaco.infrastructure.config.db.mapper;

import com.lsoftware.spaco.entity.tenant.model.Tenant;
import com.lsoftware.spaco.infrastructure.config.db.schema.TenantEntity;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Map;

@Component
public class TenantMapper {

    public Tenant toDomain(TenantEntity e) {
        return new Tenant(e.getId(), e.getSlug(), e.getName(), ZoneId.of(e.getTimezone()),
                e.getContactPhone(), e.getAddress(), e.getSlotStepMin(), e.getMinBookingLeadMin(),
                e.getBranding() != null ? e.getBranding() : Map.of());
    }
}
