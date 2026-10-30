package com.lsoftware.spaco.infrastructure.professional.dto;

import com.lsoftware.spaco.entity.professional.model.Professional;

public record ProfessionalResponse(Long id, String displayName, String title, String bio, String photoUrl,
                                   Long memberId, boolean active) {
    public static ProfessionalResponse from(Professional p, String tenantSlug) {
        return new ProfessionalResponse(p.id(), p.displayName(), p.title(), p.bio(),
                p.photoPath() == null ? null : "/media/" + tenantSlug + "/" + p.photoPath(), p.memberId(), p.active());
    }
}
