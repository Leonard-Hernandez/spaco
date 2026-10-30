package com.lsoftware.spaco.entity.consent.gateway;

import com.lsoftware.spaco.entity.consent.model.ConsentTemplate;
import com.lsoftware.spaco.entity.consent.model.ConsentType;

import java.util.List;
import java.util.Optional;

public interface ConsentTemplateGateway {
    ConsentTemplate save(ConsentTemplate template);
    /** Highest active version of that type. */
    Optional<ConsentTemplate> findCurrent(ConsentType type);
    List<ConsentTemplate> findAll();
}
