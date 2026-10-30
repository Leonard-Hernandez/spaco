package com.lsoftware.spaco.infrastructure.consent.gateway;

import com.lsoftware.spaco.entity.consent.gateway.ConsentGateway;
import com.lsoftware.spaco.entity.consent.model.Consent;
import com.lsoftware.spaco.entity.consent.model.ConsentType;
import com.lsoftware.spaco.infrastructure.config.db.mapper.ClinicalMapper;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaConsentRepository;
import com.lsoftware.spaco.infrastructure.config.db.schema.ConsentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConsentJpaGateway implements ConsentGateway {

    private final JpaConsentRepository repo;
    private final ClinicalMapper mapper;

    @Override
    public Consent save(Consent consent) {
        ConsentEntity entity = mapper.toEntity(consent);
        if (consent.id() != null) entity.setTenantId(repo.findOneById(consent.id()).orElseThrow().getTenantId());
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public List<Consent> findByPatient(Long patientId) {
        return repo.findByPatientIdOrderByAcceptedAtDesc(patientId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Consent> findValid(Long patientId, ConsentType type) {
        return repo.findByPatientIdAndTypeAndRevokedAtIsNullOrderByAcceptedAtDesc(patientId, type.name())
                .stream().map(mapper::toDomain).toList();
    }
}
