package com.lsoftware.spaco.infrastructure.professional.gateway;

import com.lsoftware.spaco.entity.professional.gateway.ProfessionalGateway;
import com.lsoftware.spaco.entity.professional.model.Professional;
import com.lsoftware.spaco.infrastructure.config.db.mapper.ScheduleMapper;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaProfessionalRepository;
import com.lsoftware.spaco.infrastructure.config.db.schema.ProfessionalEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProfessionalJpaGateway implements ProfessionalGateway {

    private final JpaProfessionalRepository repo;
    private final ScheduleMapper mapper;

    @Override
    public Professional save(Professional professional) {
        ProfessionalEntity entity = mapper.toEntity(professional);
        if (professional.id() != null) {
            ProfessionalEntity existing = repo.findOneById(professional.id()).orElseThrow();
            entity.setTenantId(existing.getTenantId());
            entity.setCreatedAt(existing.getCreatedAt());
        }
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<Professional> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }

    @Override
    public List<Professional> findAll() {
        return repo.findAllByOrderByIdAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Professional> findAllActive() {
        return repo.findByActiveTrueOrderByIdAsc().stream().map(mapper::toDomain).toList();
    }
}
