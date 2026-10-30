package com.lsoftware.spaco.infrastructure.availability.gateway;

import com.lsoftware.spaco.entity.availability.gateway.AvailabilityExceptionGateway;
import com.lsoftware.spaco.entity.availability.model.AvailabilityException;
import com.lsoftware.spaco.infrastructure.config.db.mapper.ScheduleMapper;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaAvailabilityExceptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AvailabilityExceptionJpaGateway implements AvailabilityExceptionGateway {

    private final JpaAvailabilityExceptionRepository repo;
    private final ScheduleMapper mapper;

    @Override
    public AvailabilityException save(AvailabilityException exception) {
        return mapper.toDomain(repo.save(mapper.toEntity(exception)));
    }

    @Override
    public Optional<AvailabilityException> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }

    @Override
    public List<AvailabilityException> findForProfessionalOnDate(Long professionalId, LocalDate date) {
        return repo.findForProfessionalOnDate(professionalId, date).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AvailabilityException> findByDateRange(LocalDate from, LocalDate to) {
        return repo.findByDateBetweenOrderByDateAsc(from, to).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        repo.findOneById(id).ifPresent(repo::delete);
    }
}
