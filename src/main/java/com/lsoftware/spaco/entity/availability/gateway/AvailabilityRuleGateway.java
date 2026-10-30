package com.lsoftware.spaco.entity.availability.gateway;

import com.lsoftware.spaco.entity.availability.model.AvailabilityRule;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface AvailabilityRuleGateway {
    AvailabilityRule save(AvailabilityRule rule);
    Optional<AvailabilityRule> findById(Long id);
    List<AvailabilityRule> findAll();
    List<AvailabilityRule> findActiveByProfessionalAndWeekday(Long professionalId, DayOfWeek weekday);
    void deleteById(Long id);
}
