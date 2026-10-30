package com.lsoftware.spaco.infrastructure.consent.gateway;

import com.lsoftware.spaco.entity.consent.gateway.ConsentTemplateGateway;
import com.lsoftware.spaco.entity.consent.model.ConsentTemplate;
import com.lsoftware.spaco.entity.consent.model.ConsentType;
import com.lsoftware.spaco.infrastructure.config.db.mapper.ClinicalMapper;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaConsentTemplateRepository;
import com.lsoftware.spaco.infrastructure.config.db.schema.ConsentTemplateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ConsentTemplateJpaGateway implements ConsentTemplateGateway {

    private final JpaConsentTemplateRepository repo;
    private final ClinicalMapper mapper;

    /** Always a new row: a version is never edited in place. */
    @Override
    public ConsentTemplate save(ConsentTemplate template) {
        return mapper.toDomain(repo.save(ConsentTemplateEntity.builder()
                .type(template.type().name()).version(template.version()).title(template.title())
                .body(template.body()).active(template.active())
                .build()));
    }

    @Override
    public Optional<ConsentTemplate> findCurrent(ConsentType type) {
        return repo.findFirstByTypeAndActiveTrueOrderByVersionDesc(type.name()).map(mapper::toDomain);
    }

    @Override
    public List<ConsentTemplate> findAll() {
        return repo.findAllByOrderByTypeAscVersionDesc().stream().map(mapper::toDomain).toList();
    }
}
