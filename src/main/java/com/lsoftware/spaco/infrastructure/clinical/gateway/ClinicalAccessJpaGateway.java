package com.lsoftware.spaco.infrastructure.clinical.gateway;

import com.lsoftware.spaco.entity.clinical.gateway.ClinicalAccessGateway;
import com.lsoftware.spaco.entity.clinical.model.Actor;
import com.lsoftware.spaco.entity.clinical.model.ClinicalAccess;
import com.lsoftware.spaco.infrastructure.config.db.mapper.ClinicalMapper;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaClinicalAccessRepository;
import com.lsoftware.spaco.infrastructure.config.db.schema.ClinicalAccessEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ClinicalAccessJpaGateway implements ClinicalAccessGateway {

    private final JpaClinicalAccessRepository repo;
    private final ClinicalMapper mapper;

    @Override
    public void record(Long patientId, Actor actor, String action) {
        repo.save(ClinicalAccessEntity.builder()
                .patientId(patientId).actorSub(actor.sub()).actorName(actor.name())
                .channel(actor.channel().name()).action(action)
                .build());
    }

    @Override
    public List<ClinicalAccess> findByPatient(Long patientId, int limit) {
        return repo.findByPatientIdOrderByAtDesc(patientId, Limit.of(limit)).stream().map(mapper::toDomain).toList();
    }
}
