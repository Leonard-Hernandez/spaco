package com.lsoftware.spaco.entity.treatment.model;

import java.util.List;

/**
 * A service the spa offers. price is in whole pesos; null while the spa has not published it.
 * contraindications is checked against the patient's clinical record before treating.
 */
public record Treatment(
        Long id,
        Long categoryId,
        String categorySlug,
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
        String imagePath,
        int position,
        boolean active
) {
    public Treatment withImage(String path) {
        return new Treatment(id, categoryId, categorySlug, categoryName, formulaNo, name, slug, tagline,
                description, benefits, composition, contraindications, durationMin, bufferMin, price, path,
                position, active);
    }

    /** Minutes the professional is busy: the treatment plus preparing the room. */
    public int blockMin() {
        return durationMin + bufferMin;
    }
}
