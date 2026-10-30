package com.lsoftware.spaco.infrastructure.member.dto;

import com.lsoftware.spaco.entity.member.model.MemberRole;
import com.lsoftware.spaco.entity.member.model.TenantMember;

public record StaffResponse(String email, String name, MemberRole role) {
    public static StaffResponse from(TenantMember m) {
        return new StaffResponse(m.email(), m.name(), m.role());
    }
}
