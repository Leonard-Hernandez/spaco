package com.lsoftware.spaco.infrastructure.config.db.repository;

import com.lsoftware.spaco.infrastructure.config.db.schema.SessionPhotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Lookups by id go through findOneById (a JPQL query) so the @TenantId restriction applies;
// em.find() behind findById() does not filter by tenant.
public interface JpaSessionPhotoRepository extends JpaRepository<SessionPhotoEntity, Long> {
    Optional<SessionPhotoEntity> findOneById(Long id);
    List<SessionPhotoEntity> findBySessionNoteIdInOrderByIdAsc(Collection<Long> sessionNoteIds);
}
