package com.lsoftware.spaco.usecase.tenant;

import com.lsoftware.spaco.entity.patient.model.PhoneNumber;
import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.entity.tenant.gateway.TenantGateway;
import com.lsoftware.spaco.entity.tenant.model.Tenant;
import com.lsoftware.spaco.usecase.tenant.dto.TenantProfileCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The owner's view of the spa's public profile. Only the keys it manages are touched in the
 * branding map; logo, wordmark, theme, heroTreatment... are kept as they are.
 */
@Component
@RequiredArgsConstructor
public class ManageTenantProfileUseCase {

    private final CurrentTenantGateway current;
    private final TenantGateway tenants;

    @Transactional(readOnly = true)
    public Tenant get() {
        return current.current();
    }

    @Transactional
    public Tenant update(TenantProfileCommand cmd) {
        Tenant tenant = current.current();
        String phone = blankToNull(cmd.phone()) == null ? null : PhoneNumber.toE164(cmd.phone());
        String address = blankToNull(cmd.address());

        Map<String, Object> branding = new LinkedHashMap<>(tenant.branding());
        put(branding, "whatsapp", phone);
        put(branding, "email", blankToNull(cmd.email()));
        put(branding, "address", address);
        put(branding, "city", blankToNull(cmd.city()));
        put(branding, "tagline", blankToNull(cmd.tagline()));
        put(branding, "intro", blankToNull(cmd.intro()));
        put(branding, "instagram", normalizeInstagram(cmd.instagram()));
        put(branding, "cancellationNote", blankToNull(cmd.cancellationNote()));
        // Opening hours come from the availability rules; the old free text would only go stale.
        branding.remove("hours");

        return tenants.updateProfile(tenant.id(), phone, address, branding);
    }

    private static void put(Map<String, Object> map, String key, String value) {
        if (value == null) map.remove(key); else map.put(key, value);
    }

    /** Keeps the handle with a single leading @. */
    static String normalizeInstagram(String raw) {
        String v = blankToNull(raw);
        return v == null ? null : "@" + v.replaceFirst("^@+", "");
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.strip();
    }
}
