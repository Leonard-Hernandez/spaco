package com.lsoftware.spaco.infrastructure.tenant.dto;

import com.lsoftware.spaco.entity.tenant.model.Tenant;

import java.util.Map;

/** Editable profile; seed placeholders such as "[DIRECCIÓN]" come back as null. */
public record TenantProfileResponse(
        String phone,
        String email,
        String address,
        String city,
        String tagline,
        String intro,
        String instagram,
        String cancellationNote
) {
    public static TenantProfileResponse from(Tenant t) {
        Map<String, Object> b = t.branding();
        String phone = real(t.contactPhone());
        return new TenantProfileResponse(
                phone != null ? phone : real(text(b, "whatsapp")),
                real(text(b, "email")),
                real(text(b, "address")) != null ? real(text(b, "address")) : real(t.address()),
                real(text(b, "city")),
                real(text(b, "tagline")),
                real(text(b, "intro")),
                real(text(b, "instagram")),
                real(text(b, "cancellationNote")));
    }

    private static String text(Map<String, Object> b, String key) {
        return b.get(key) instanceof String s ? s : null;
    }

    static String real(String v) {
        if (v == null) return null;
        String s = v.strip();
        return s.isEmpty() || (s.startsWith("[") && s.endsWith("]")) ? null : s;
    }
}
