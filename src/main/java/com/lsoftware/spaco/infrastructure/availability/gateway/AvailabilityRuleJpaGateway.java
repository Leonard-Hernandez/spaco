package com.lsoftware.spaco.infrastructure.availability.gateway;

import com.lsoftware.spaco.entity.availability.gateway.AvailabilityRuleGateway;
import com.lsoftware.spaco.entity.availability.model.AvailabilityRule;
import com.lsoftware.spaco.infrastructure.config.db.mapper.ScheduleMapper;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaAvailabilityRuleRepository;
import com.lsoftware.spaco.infrastructure.config.db.schema.AvailabilityRuleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AvailabilityRuleJpaGateway implements AvailabilityRuleGateway {

    private final JpaAvailabilityRuleRepository repo;
    private final ScheduleMapper mapper;

    @Override
    public AvailabilityRule save(AvailabilityRule rule) {
        AvailabilityRuleEntity entity = mapper.toEntity(rule);
        if (rule.id() != null) entity.setTenantId(repo.findOneById(rule.id()).orElseThrow().getTenantId());
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Optional<AvailabilityRule> findById(Long id) {
        return repo.findOneById(id).map(mapper::toDomain);
    }

    @Override
    public List<AvailabilityRule> findAll() {
        return repo.findAllByOrderByProfessionalIdAscWeekdayAscStartTimeAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AvailabilityRule> findActiveByProfessionalAndWeekday(Long professionalId, DayOfWeek weekday) {
        return repo.findByProfessionalIdAndWeekdayAndActiveTrue(professionalId, (short) weekday.getValue())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        repo.findOneById(id).ifPresent(repo::delete);
    }
}
