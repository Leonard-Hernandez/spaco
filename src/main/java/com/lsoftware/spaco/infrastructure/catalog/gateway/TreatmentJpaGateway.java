package com.lsoftware.spaco.infrastructure.catalog.gateway;

import com.lsoftware.spaco.entity.treatment.exception.CategoryNotFoundException;
import com.lsoftware.spaco.entity.treatment.gateway.TreatmentGateway;
import com.lsoftware.spaco.entity.treatment.model.Treatment;
import com.lsoftware.spaco.infrastructure.config.db.mapper.TreatmentMapper;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaTreatmentCategoryRepository;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaTreatmentRepository;
import com.lsoftware.spaco.infrastructure.config.db.schema.TreatmentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TreatmentJpaGateway implements TreatmentGateway {

    private final JpaTreatmentRepository repo;
    private final JpaTreatmentCategoryRepository categories;
    private final TreatmentMapper mapper;

    @Override
    public Treatment save(Treatment treatment) {
        TreatmentEntity entity = mapper.toEntity(treatment);
        entity.setCategory(categories.findOneById(treatment.categoryId()).orElseThrow(CategoryNotFoundException::new));
        if (treatment.id() != null) {
            TreatmentEntity existing = repo.findOneById(treatment.id()).orElseThrow();
            entity.setTenantId(existing.getTenantId());
            entity.setCreatedAt(existing.getCreatedAt());
        }
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<Treatment> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Treatment> findBySlug(String slug) {
        return repo.findBySlug(slug).map(mapper::toDomain);
    }

    @Override
    public List<Treatment> findAll() {
        return repo.findAllByOrderByPositionAscNameAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsBySlug(String slug, Long excludingId) {
        return repo.findBySlug(slug).filter(t -> !Objects.equals(t.getId(), excludingId)).isPresent();
    }

    @Override
    public boolean existsInCategory(Long categoryId) {
        return repo.existsByCategoryId(categoryId);
    }
}
