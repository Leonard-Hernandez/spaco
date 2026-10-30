package com.lsoftware.spaco.entity.tenant.model;

import java.time.ZoneId;
import java.util.Map;

public record Tenant(
        Long id,
        String slug,
        String name,
        ZoneId timezone,
        String contactPhone,
        String address,
        /** Minutes between the start times offered to patients. */
        int slotStepMin,
        /** How far ahead a booking must start, so the spa has time to confirm it. */
        int minBookingLeadMin,
        /** Theme overrides and public profile (tagline, hours, whatsapp...) read by the site. */
        Map<String, Object> branding
) {}
