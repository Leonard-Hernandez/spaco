package com.lsoftware.spaco.infrastructure.config.db.repository;

import com.lsoftware.spaco.infrastructure.config.db.schema.ConsentTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaConsentTemplateRepository extends JpaRepository<ConsentTemplateEntity, Long> {
    Optional<ConsentTemplateEntity> findFirstByTypeAndActiveTrueOrderByVersionDesc(String type);
    List<ConsentTemplateEntity> findAllByOrderByTypeAscVersionDesc();
}
