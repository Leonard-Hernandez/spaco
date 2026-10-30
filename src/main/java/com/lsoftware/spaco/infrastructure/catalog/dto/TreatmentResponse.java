package com.lsoftware.spaco.infrastructure.catalog.dto;

import com.lsoftware.spaco.entity.treatment.model.CompositionLine;
import com.lsoftware.spaco.entity.treatment.model.Treatment;

import java.util.List;

/** imageUrl is relative to the API (/media/{tenant}/{file}). price null = "Consultar". */
public record TreatmentResponse(
        Long id,
        Long categoryId,
        String category,
        String categoryName,
        Integer formulaNo,
        String name,
        String slug,
        String tagline,
        String description,
        List<String> benefits,
        List<CompositionLine> composition,
        String contraindications,
        int durationMin,
        int bufferMin,
        Long price,
        String imageUrl,
        int position,
        boolean active
) {
    public static TreatmentResponse from(Treatment t, String tenantSlug) {
        return new TreatmentResponse(t.id(), t.categoryId(), t.categorySlug(), t.categoryName(), t.formulaNo(),
                t.name(), t.slug(), t.tagline(), t.description(), t.benefits(), t.composition(),
                t.contraindications(), t.durationMin(), t.bufferMin(), t.price(),
                t.imagePath() == null ? null : "/media/" + tenantSlug + "/" + t.imagePath(), t.position(), t.active());
    }
}
