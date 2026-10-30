package com.lsoftware.spaco.usecase.catalog.dto;

import com.lsoftware.spaco.entity.treatment.model.CompositionLine;

import java.util.List;

public record TreatmentCommand(
        Long categoryId,
        Integer formulaNo,
        String name,
        String tagline,
        String description,
        List<String> benefits,
        List<CompositionLine> composition,
        String contraindications,
        int durationMin,
        int bufferMin,
        Long price,
        int position,
        boolean active
) {}
