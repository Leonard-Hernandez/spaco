package com.lsoftware.spaco.usecase.catalog;

import com.lsoftware.spaco.entity.media.gateway.PublicImageGateway;
import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.entity.treatment.exception.CategoryNotEmptyException;
import com.lsoftware.spaco.entity.treatment.exception.CategoryNotFoundException;
import com.lsoftware.spaco.entity.treatment.exception.DuplicateSlugException;
import com.lsoftware.spaco.entity.treatment.exception.TreatmentNotFoundException;
import com.lsoftware.spaco.entity.treatment.gateway.TreatmentCategoryGateway;
import com.lsoftware.spaco.entity.treatment.gateway.TreatmentGateway;
import com.lsoftware.spaco.entity.treatment.model.Treatment;
import com.lsoftware.spaco.entity.treatment.model.TreatmentCategory;
import com.lsoftware.spaco.usecase.catalog.dto.TreatmentCommand;
import com.lsoftware.spaco.usecase.media.ImageFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The owner's catalogue editor: categories, treatments and their photos. */
@Component
@RequiredArgsConstructor
public class ManageCatalogUseCase {

    private final TreatmentCategoryGateway categories;
    private final TreatmentGateway treatments;
    private final PublicImageGateway images;
    private final CurrentTenantGateway tenants;

    @Transactional(readOnly = true)
    public List<TreatmentCategory> categories() {
        return categories.findAll();
    }

    @Transactional(readOnly = true)
    public List<Treatment> treatments() {
        return treatments.findAll();
    }

    @Transactional
    public TreatmentCategory saveCategory(Long id, String name, String ink, int position, boolean active) {
        String slug = Slugs.of(name);
        if (categories.existsBySlug(slug, id)) throw new DuplicateSlugException();
        if (id != null) categories.findById(id).orElseThrow(CategoryNotFoundException::new);
        return categories.save(new TreatmentCategory(id, name.strip(), slug, ink, position, active));
    }

    @Transactional
    public void deleteCategory(Long id) {
        categories.findById(id).orElseThrow(CategoryNotFoundException::new);
        if (treatments.existsInCategory(id)) throw new CategoryNotEmptyException();
        categories.deleteById(id);
    }

    @Transactional
    public Treatment saveTreatment(Long id, TreatmentCommand cmd) {
        TreatmentCategory category = categories.findById(cmd.categoryId()).orElseThrow(CategoryNotFoundException::new);
        String slug = Slugs.of(cmd.name());
        if (treatments.existsBySlug(slug, id)) throw new DuplicateSlugException();
        String image = id == null ? null
                : treatments.findById(id).orElseThrow(TreatmentNotFoundException::new).imagePath();
        return treatments.save(new Treatment(id, category.id(), category.slug(), category.name(), cmd.formulaNo(),
                cmd.name().strip(), slug, cmd.tagline(), cmd.description(),
                cmd.benefits() != null ? cmd.benefits() : List.of(),
                cmd.composition() != null ? cmd.composition() : List.of(),
                cmd.contraindications(), cmd.durationMin(), cmd.bufferMin(), cmd.price(), image,
                cmd.position(), cmd.active()));
    }

    @Transactional
    public Treatment setImage(Long id, byte[] content) {
        Treatment treatment = treatments.findById(id).orElseThrow(TreatmentNotFoundException::new);
        String ext = ImageFormat.extensionOf(content);
        String slug = tenants.current().slug();
        String file = images.store(slug, ImageFormat.withoutMetadata(content, ext), ext);
        if (treatment.imagePath() != null) images.delete(slug, treatment.imagePath());
        return treatments.save(treatment.withImage(file));
    }

    @Transactional
    public Treatment removeImage(Long id) {
        Treatment treatment = treatments.findById(id).orElseThrow(TreatmentNotFoundException::new);
        if (treatment.imagePath() != null) images.delete(tenants.current().slug(), treatment.imagePath());
        return treatments.save(treatment.withImage(null));
    }
}
