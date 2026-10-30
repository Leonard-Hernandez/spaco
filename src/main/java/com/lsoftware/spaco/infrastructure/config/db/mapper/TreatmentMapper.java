package com.lsoftware.spaco.infrastructure.config.db.mapper;

import com.lsoftware.spaco.entity.treatment.model.CompositionLine;
import com.lsoftware.spaco.entity.treatment.model.Treatment;
import com.lsoftware.spaco.entity.treatment.model.TreatmentCategory;
import com.lsoftware.spaco.infrastructure.config.db.schema.TreatmentCategoryEntity;
import com.lsoftware.spaco.infrastructure.config.db.schema.TreatmentEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TreatmentMapper {

    public TreatmentCategory toDomain(TreatmentCategoryEntity e) {
        return new TreatmentCategory(e.getId(), e.getName(), e.getSlug(), e.getInk(), e.getPosition(), e.isActive());
    }

    public TreatmentCategoryEntity toEntity(TreatmentCategory c) {
        return TreatmentCategoryEntity.builder()
                .id(c.id()).name(c.name()).slug(c.slug()).ink(c.ink()).position(c.position()).active(c.active())
                .build();
    }

    public Treatment toDomain(TreatmentEntity e) {
        List<CompositionLine> composition = e.getComposition() == null ? List.of()
                : e.getComposition().stream().map(m -> new CompositionLine(m.get("label"), m.get("value"))).toList();
        return new Treatment(e.getId(), e.getCategory().getId(), e.getCategory().getSlug(), e.getCategory().getName(),
                e.getFormulaNo(), e.getName(), e.getSlug(), e.getTagline(), e.getDescription(),
                e.getBenefits() == null ? List.of() : List.copyOf(e.getBenefits()), composition,
                e.getContraindications(), e.getDurationMin(), e.getBufferMin(), e.getPrice(), e.getImagePath(),
                e.getPosition(), e.isActive());
    }

    /** The category reference is set by the gateway. */
    public TreatmentEntity toEntity(Treatment t) {
        return TreatmentEntity.builder()
                .id(t.id()).formulaNo(t.formulaNo()).name(t.name()).slug(t.slug()).tagline(t.tagline())
                .description(t.description()).benefits(t.benefits())
                .composition(t.composition().stream()
                        .map(c -> Map.of("label", c.label(), "value", c.value()))
                        .toList())
                .contraindications(t.contraindications()).durationMin(t.durationMin()).bufferMin(t.bufferMin())
                .price(t.price()).imagePath(t.imagePath()).position(t.position()).active(t.active())
                .build();
    }
}
