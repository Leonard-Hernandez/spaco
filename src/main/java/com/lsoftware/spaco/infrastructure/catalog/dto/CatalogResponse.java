package com.lsoftware.spaco.infrastructure.catalog.dto;

import com.lsoftware.spaco.entity.treatment.model.TreatmentCategory;

import java.util.List;

public record CatalogResponse(List<TreatmentCategory> categories, List<TreatmentResponse> treatments) {}
