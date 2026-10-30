package com.lsoftware.spaco.entity.consent.gateway;

import com.lsoftware.spaco.entity.consent.model.Consent;
import com.lsoftware.spaco.entity.consent.model.ConsentType;

import java.util.List;

public interface ConsentGateway {
    Consent save(Consent consent);
    /** Newest first, revoked ones included (the history matters). */
    List<Consent> findByPatient(Long patientId);
    List<Consent> findValid(Long patientId, ConsentType type);
}
