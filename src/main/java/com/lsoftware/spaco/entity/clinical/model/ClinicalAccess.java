package com.lsoftware.spaco.entity.clinical.model;

import java.time.OffsetDateTime;

public record ClinicalAccess(Long id, Long patientId, String actorSub, String actorName, AccessChannel channel,
                             String action, OffsetDateTime at) {}
