package com.lsoftware.spaco.entity.treatment.gateway;

import com.lsoftware.spaco.entity.treatment.model.TreatmentCategory;

import java.util.List;
import java.util.Optional;

public interface TreatmentCategoryGateway {
    TreatmentCategory save(TreatmentCategory category);
    Optional<TreatmentCategory> findById(Long id);
    List<TreatmentCategory> findAll();
    boolean existsBySlug(String slug, Long excludingId);
    void deleteById(Long id);
}
