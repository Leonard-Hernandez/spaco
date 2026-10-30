package com.lsoftware.spaco.infrastructure.clinical.gateway;

import com.lsoftware.spaco.entity.clinical.gateway.SessionNoteGateway;
import com.lsoftware.spaco.entity.clinical.model.NoteMatch;
import com.lsoftware.spaco.entity.clinical.model.PhotoKind;
import com.lsoftware.spaco.entity.clinical.model.SessionNote;
import com.lsoftware.spaco.entity.clinical.model.SessionPhoto;
import com.lsoftware.spaco.infrastructure.config.db.mapper.ClinicalMapper;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaProfessionalRepository;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaSessionNoteRepository;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaSessionPhotoRepository;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaTreatmentRepository;
import com.lsoftware.spaco.infrastructure.config.db.schema.ProfessionalEntity;
import com.lsoftware.spaco.infrastructure.config.db.schema.SessionNoteEntity;
import com.lsoftware.spaco.infrastructure.config.db.schema.SessionPhotoEntity;
import com.lsoftware.spaco.infrastructure.config.db.schema.TreatmentEntity;
import com.lsoftware.spaco.infrastructure.config.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SessionNoteJpaGateway implements SessionNoteGateway {

    private final JpaSessionNoteRepository repo;
    private final JpaSessionPhotoRepository photos;
    private final JpaTreatmentRepository treatments;
    private final JpaProfessionalRepository professionals;
    private final ClinicalMapper mapper;
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public SessionNote save(SessionNote note) {
        SessionNoteEntity entity = mapper.toEntity(note);
        if (note.id() != null) {
            SessionNoteEntity existing = repo.findOneById(note.id()).orElseThrow();
            entity.setTenantId(existing.getTenantId());
            entity.setCreatedAt(existing.getCreatedAt());
        }
        return withNames(List.of(repo.save(entity))).getFirst();
    }

    @Override
    public Optional<SessionNote> findById(Long id) {
        return repo.findOneById(id).map(e -> withNames(List.of(e)).getFirst());
    }

    @Override
    public Optional<SessionNote> findByAppointment(Long appointmentId) {
        return repo.findByAppointmentId(appointmentId).map(e -> withNames(List.of(e)).getFirst());
    }

    @Override
    public List<SessionNote> findByPatient(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        return withNames(repo.findByPatient(patientId, from, to));
    }

    @Override
    public SessionPhoto addPhoto(Long sessionNoteId, PhotoKind kind, String fileName, String caption) {
        return mapper.toDomain(photos.save(SessionPhotoEntity.builder()
                .sessionNoteId(sessionNoteId).kind(kind.name()).fileName(fileName).caption(caption).build()));
    }

    @Override
    public Optional<SessionPhoto> findPhoto(Long photoId) {
        return photos.findOneById(photoId).map(mapper::toDomain);
    }

    @Override
    public void deletePhoto(Long photoId) {
        photos.findOneById(photoId).ifPresent(photos::delete);
    }

    /** Plain SQL (full-text index), so the tenant is filtered explicitly. */
    @Override
    public List<NoteMatch> search(String text, Long patientId, int limit) {
        String sql = """
                WITH q AS (SELECT websearch_to_tsquery('spanish', :text) AS query)
                SELECT n.id, n.patient_id, p.full_name, n.performed_at, t.name AS treatment,
                       ts_headline('spanish', concat_ws(' · ', n.products, n.parameters, n.observations, n.recommendations),
                                   q.query, 'MaxFragments=2, MaxWords=18, MinWords=6, StartSel=«, StopSel=»') AS excerpt
                FROM session_note n
                JOIN patient p ON p.id = n.patient_id
                LEFT JOIN treatment t ON t.id = n.treatment_id
                CROSS JOIN q
                WHERE n.tenant_id = :tenant
                  AND (CAST(:patient AS BIGINT) IS NULL OR n.patient_id = :patient)
                  AND to_tsvector('spanish', coalesce(n.products, '') || ' ' || coalesce(n.parameters, '') || ' ' ||
                      coalesce(n.observations, '') || ' ' || coalesce(n.recommendations, '')) @@ q.query
                ORDER BY n.performed_at DESC
                LIMIT :limit
                """;
        return jdbc.query(sql, new MapSqlParameterSource()
                        .addValue("text", text)
                        .addValue("tenant", TenantContext.current().orElseThrow().id())
                        .addValue("patient", patientId)
                        .addValue("limit", limit),
                (rs, i) -> new NoteMatch(rs.getLong("id"), rs.getLong("patient_id"), rs.getString("full_name"),
                        rs.getObject("performed_at", OffsetDateTime.class).withOffsetSameInstant(ZoneOffset.UTC),
                        rs.getString("treatment"), rs.getString("excerpt")));
    }

    /** Treatment, professional and photos of a page of notes, in three queries. */
    private List<SessionNote> withNames(List<SessionNoteEntity> notes) {
        if (notes.isEmpty()) return List.of();
        Map<Long, String> treatmentNames = treatments.findAllById(ids(notes, SessionNoteEntity::getTreatmentId)).stream()
                .collect(Collectors.toMap(TreatmentEntity::getId, TreatmentEntity::getName));
        Map<Long, String> professionalNames = professionals.findAllById(ids(notes, SessionNoteEntity::getProfessionalId))
                .stream().collect(Collectors.toMap(ProfessionalEntity::getId, ProfessionalEntity::getDisplayName));
        Map<Long, List<SessionPhoto>> photosByNote = photos.findBySessionNoteIdInOrderByIdAsc(
                        notes.stream().map(SessionNoteEntity::getId).toList()).stream()
                .map(mapper::toDomain)
                .collect(Collectors.groupingBy(SessionPhoto::sessionNoteId));
        return notes.stream().map(n -> mapper.toDomain(n, treatmentNames.get(n.getTreatmentId()),
                professionalNames.get(n.getProfessionalId()), photosByNote.getOrDefault(n.getId(), List.of()))).toList();
    }

    private static List<Long> ids(List<SessionNoteEntity> notes, java.util.function.Function<SessionNoteEntity, Long> f) {
        return notes.stream().map(f).filter(java.util.Objects::nonNull).distinct().toList();
    }
}
