package com.lsoftware.spaco.usecase.professional;

import com.lsoftware.spaco.entity.media.gateway.PublicImageGateway;
import com.lsoftware.spaco.entity.professional.exception.ProfessionalNotFoundException;
import com.lsoftware.spaco.entity.professional.gateway.ProfessionalGateway;
import com.lsoftware.spaco.entity.professional.model.Professional;
import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.usecase.media.ImageFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ManageProfessionalUseCase {

    private final ProfessionalGateway professionals;
    private final PublicImageGateway images;
    private final CurrentTenantGateway tenants;

    @Transactional(readOnly = true)
    public List<Professional> list() {
        return professionals.findAll();
    }

    @Transactional(readOnly = true)
    public List<Professional> listActive() {
        return professionals.findAllActive();
    }

    @Transactional
    public Professional save(Long id, String displayName, String title, String bio, Long memberId, boolean active) {
        String photo = id == null ? null
                : professionals.findById(id).orElseThrow(ProfessionalNotFoundException::new).photoPath();
        return professionals.save(new Professional(id, displayName.strip(), title, bio, photo, memberId, active));
    }

    @Transactional
    public Professional setPhoto(Long id, byte[] content) {
        Professional professional = professionals.findById(id).orElseThrow(ProfessionalNotFoundException::new);
        String ext = ImageFormat.extensionOf(content);
        String slug = tenants.current().slug();
        String file = images.store(slug, ImageFormat.withoutMetadata(content, ext), ext);
        if (professional.photoPath() != null) images.delete(slug, professional.photoPath());
        return professionals.save(professional.withPhoto(file));
    }
}
