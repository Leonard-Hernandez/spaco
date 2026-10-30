package com.lsoftware.spaco.usecase.clinical;

import com.lsoftware.spaco.entity.clinical.gateway.ClinicalAccessGateway;
import com.lsoftware.spaco.entity.clinical.model.Actor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Every read or change of clinical data leaves a trace: who, from where, what. */
@Component
@RequiredArgsConstructor
public class ClinicalAudit {

    private final ClinicalAccessGateway log;

    public void record(Long patientId, Actor actor, String action) {
        log.record(patientId, actor, action);
    }
}
