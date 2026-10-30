package com.lsoftware.spaco.infrastructure.config.security;

import com.lsoftware.spaco.entity.clinical.model.AccessChannel;
import com.lsoftware.spaco.entity.clinical.model.Actor;
import com.lsoftware.spaco.entity.member.model.TenantMember;

public final class Actors {

    private Actors() {}

    public static Actor staff(TenantMember member, AccessChannel channel) {
        return new Actor(member.authcoSub(), member.name() != null ? member.name() : member.email(), channel);
    }
}
