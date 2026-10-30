package com.lsoftware.spaco.usecase.catalog;

import com.lsoftware.spaco.entity.treatment.exception.TreatmentNotFoundException;
import com.lsoftware.spaco.entity.treatment.gateway.TreatmentCategoryGateway;
import com.lsoftware.spaco.entity.treatment.gateway.TreatmentGateway;
import com.lsoftware.spaco.entity.treatment.model.Treatment;
import com.lsoftware.spaco.entity.treatment.model.TreatmentCategory;
import com.lsoftware.spaco.usecase.catalog.dto.Catalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/** What the public site shows: active categories with their active treatments. */
@Component
@RequiredArgsConstructor
public class GetCatalogUseCase {

    private final TreatmentCategoryGateway categories;
    private final TreatmentGateway treatments;

    @Transactional(readOnly = true)
    public Catalog execute() {
        var active = categories.findAll().stream().filter(TreatmentCategory::active).toList();
        Set<Long> ids = active.stream().map(TreatmentCategory::id).collect(Collectors.toSet());
        var shown = treatments.findAll().stream()
                .filter(t -> t.active() && ids.contains(t.categoryId()))
                .toList();
        return new Catalog(active, shown);
    }

    @Transactional(readOnly = true)
    public Treatment bySlug(String slug) {
        return treatments.findBySlug(slug).filter(Treatment::active).orElseThrow(TreatmentNotFoundException::new);
    }
}
