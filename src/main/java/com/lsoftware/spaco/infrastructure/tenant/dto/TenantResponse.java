package com.lsoftware.spaco.infrastructure.tenant.dto;

import com.lsoftware.spaco.entity.availability.model.OpeningDay;
import com.lsoftware.spaco.entity.tenant.model.Tenant;

import java.util.List;
import java.util.Map;

public record TenantResponse(
        String slug,
        String name,
        String timezone,
        String contactPhone,
        String address,
        Map<String, Object> branding,
        /** Weekly opening hours derived from the availability rules (closed days are absent). */
        List<OpeningDayResponse> openingHours
) {
    public static TenantResponse from(Tenant t, List<OpeningDay> openingHours) {
        return new TenantResponse(t.slug(), t.name(), t.timezone().getId(), t.contactPhone(),
                t.address(), t.branding(), openingHours.stream().map(OpeningDayResponse::from).toList());
    }
}
