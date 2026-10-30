package com.lsoftware.spaco.infrastructure.catalog.gateway;

import com.lsoftware.spaco.entity.treatment.gateway.TreatmentCategoryGateway;
import com.lsoftware.spaco.entity.treatment.model.TreatmentCategory;
import com.lsoftware.spaco.infrastructure.config.db.mapper.TreatmentMapper;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaTreatmentCategoryRepository;
import com.lsoftware.spaco.infrastructure.config.db.schema.TreatmentCategoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TreatmentCategoryJpaGateway implements TreatmentCategoryGateway {

    private final JpaTreatmentCategoryRepository repo;
    private final TreatmentMapper mapper;

    @Override
    public TreatmentCategory save(TreatmentCategory category) {
        TreatmentCategoryEntity entity = mapper.toEntity(category);
        if (category.id() != null) entity.setTenantId(repo.findOneById(category.id()).orElseThrow().getTenantId());
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<TreatmentCategory> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }

    @Override
    public List<TreatmentCategory> findAll() {
        return repo.findAllByOrderByPositionAscNameAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsBySlug(String slug, Long excludingId) {
        return repo.findBySlug(slug).filter(c -> !Objects.equals(c.getId(), excludingId)).isPresent();
    }

    @Override
    public void deleteById(Long id) {
        repo.findOneById(id).ifPresent(repo::delete);
    }
}
