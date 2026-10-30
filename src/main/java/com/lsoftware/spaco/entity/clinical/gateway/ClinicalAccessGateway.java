package com.lsoftware.spaco.entity.clinical.gateway;

import com.lsoftware.spaco.entity.clinical.model.Actor;
import com.lsoftware.spaco.entity.clinical.model.ClinicalAccess;

import java.util.List;

public interface ClinicalAccessGateway {
    void record(Long patientId, Actor actor, String action);
    List<ClinicalAccess> findByPatient(Long patientId, int limit);
}
