package com.lsoftware.spaco.entity.treatment.gateway;

import com.lsoftware.spaco.entity.treatment.model.Treatment;

import java.util.List;
import java.util.Optional;

public interface TreatmentGateway {
    Treatment save(Treatment treatment);
    Optional<Treatment> findById(Long id);
    Optional<Treatment> findBySlug(String slug);
    List<Treatment> findAll();
    boolean existsBySlug(String slug, Long excludingId);
    boolean existsInCategory(Long categoryId);
}
