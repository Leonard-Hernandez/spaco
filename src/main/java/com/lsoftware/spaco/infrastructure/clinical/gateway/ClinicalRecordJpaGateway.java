package com.lsoftware.spaco.infrastructure.clinical.gateway;

import com.lsoftware.spaco.entity.clinical.gateway.ClinicalRecordGateway;
import com.lsoftware.spaco.entity.clinical.model.ClinicalRecord;
import com.lsoftware.spaco.infrastructure.config.db.mapper.ClinicalMapper;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaClinicalRecordRepository;
import com.lsoftware.spaco.infrastructure.config.db.schema.ClinicalRecordEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ClinicalRecordJpaGateway implements ClinicalRecordGateway {

    private final JpaClinicalRecordRepository repo;
    private final ClinicalMapper mapper;

    @Override
    public ClinicalRecord save(ClinicalRecord record) {
        ClinicalRecordEntity entity = mapper.toEntity(record);
        repo.findOneByPatientId(record.patientId()).ifPresent(existing -> entity.setTenantId(existing.getTenantId()));
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<ClinicalRecord> findByPatient(Long patientId) {
        return repo.findOneByPatientId(patientId).map(mapper::toDomain);
    }
}
