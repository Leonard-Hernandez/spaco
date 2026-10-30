package com.lsoftware.spaco.entity.consent.model;

import java.time.OffsetDateTime;

public record Consent(
        Long id,
        Long patientId,
        ConsentType type,
        int templateVersion,
        Long treatmentId,
        OffsetDateTime acceptedAt,
        OffsetDateTime revokedAt,
        ConsentChannel channel,
        String recordedBy
) {
    public boolean valid() {
        return revokedAt == null;
    }

    public Consent revoke(OffsetDateTime at) {
        return new Consent(id, patientId, type, templateVersion, treatmentId, acceptedAt, at, channel, recordedBy);
    }
}
