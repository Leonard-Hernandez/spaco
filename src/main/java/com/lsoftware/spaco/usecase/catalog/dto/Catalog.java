package com.lsoftware.spaco.usecase.catalog.dto;

import com.lsoftware.spaco.entity.treatment.model.Treatment;
import com.lsoftware.spaco.entity.treatment.model.TreatmentCategory;

import java.util.List;

public record Catalog(List<TreatmentCategory> categories, List<Treatment> treatments) {}
