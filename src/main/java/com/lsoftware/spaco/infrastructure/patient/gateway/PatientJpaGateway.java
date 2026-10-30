package com.lsoftware.spaco.infrastructure.patient.gateway;

import com.lsoftware.spaco.entity.exception.ConflictException;
import com.lsoftware.spaco.entity.patient.gateway.PatientGateway;
import com.lsoftware.spaco.entity.patient.model.Patient;
import com.lsoftware.spaco.infrastructure.config.db.mapper.PatientMapper;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaPatientRepository;
import com.lsoftware.spaco.infrastructure.config.db.schema.PatientEntity;
import com.lsoftware.spaco.infrastructure.config.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PatientJpaGateway implements PatientGateway {

    private final JpaPatientRepository repo;
    private final PatientMapper mapper;
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Patient save(Patient patient) {
        PatientEntity entity = mapper.toEntity(patient);
        if (patient.id() != null) {
            PatientEntity existing = repo.findOneById(patient.id()).orElseThrow();
            entity.setTenantId(existing.getTenantId());
            entity.setCreatedAt(existing.getCreatedAt());
        }
        try {
            return mapper.toDomain(repo.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("PHONE_TAKEN", "Another patient already uses that phone number");
        }
    }

    @Override
    public Optional<Patient> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Patient> findByPhone(String phoneE164) {
        return repo.findByPhoneE164(phoneE164).map(mapper::toDomain);
    }

    @Override
    public Optional<Patient> findBySub(String authcoSub) {
        return repo.findByAuthcoSub(authcoSub).map(mapper::toDomain);
    }

    @Override
    public List<Patient> findUnlinkedByEmail(String email) {
        return repo.findByEmailIgnoreCaseAndAuthcoSubIsNull(email).stream().map(mapper::toDomain).toList();
    }

    /**
     * Name without accents and with typos (pg_trgm word similarity), or part of the phone or
     * email. Plain SQL, so the tenant is filtered explicitly.
     */
    @Override
    public List<Patient> search(String query, int limit) {
        if (query.isBlank()) {
            return repo.findAllByOrderByCreatedAtDesc(Limit.of(limit)).stream().map(mapper::toDomain).toList();
        }
        String digits = query.replaceAll("\\D", "");
        String sql = """
                SELECT id FROM patient
                WHERE tenant_id = :tenant
                  AND (:q <% translate(lower(full_name), 'áéíóúüñ', 'aeiouun')
                       OR translate(lower(full_name), 'áéíóúüñ', 'aeiouun') LIKE '%' || :q || '%'
                       OR (length(:digits) >= 4 AND phone_e164 LIKE '%' || :digits || '%')
                       OR lower(email) LIKE '%' || :q || '%')
                ORDER BY word_similarity(:q, translate(lower(full_name), 'áéíóúüñ', 'aeiouun')) DESC, created_at DESC
                LIMIT :limit
                """;
        String q = java.text.Normalizer.normalize(query.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        List<Long> ids = jdbc.queryForList(sql, new MapSqlParameterSource()
                .addValue("tenant", TenantContext.current().orElseThrow().id())
                .addValue("q", q)
                .addValue("digits", digits)
                .addValue("limit", limit), Long.class);
        return ids.stream().map(repo::findOneById).flatMap(Optional::stream).map(mapper::toDomain).toList();
    }
}
