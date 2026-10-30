package com.lsoftware.spaco.usecase.availability;

import com.lsoftware.spaco.entity.availability.exception.ExceptionNotFoundException;
import com.lsoftware.spaco.entity.availability.exception.InvalidTimeRangeException;
import com.lsoftware.spaco.entity.availability.exception.RuleNotFoundException;
import com.lsoftware.spaco.entity.availability.gateway.AvailabilityExceptionGateway;
import com.lsoftware.spaco.entity.availability.gateway.AvailabilityRuleGateway;
import com.lsoftware.spaco.entity.availability.model.AvailabilityException;
import com.lsoftware.spaco.entity.availability.model.AvailabilityRule;
import com.lsoftware.spaco.entity.availability.model.ExceptionType;
import com.lsoftware.spaco.entity.professional.exception.ProfessionalNotFoundException;
import com.lsoftware.spaco.entity.professional.gateway.ProfessionalGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** Weekly hours per professional, and one-off closures or extra hours. */
@Component
@RequiredArgsConstructor
public class ManageScheduleUseCase {

    private final AvailabilityRuleGateway rules;
    private final AvailabilityExceptionGateway exceptions;
    private final ProfessionalGateway professionals;

    @Transactional(readOnly = true)
    public List<AvailabilityRule> rules() {
        return rules.findAll();
    }

    @Transactional
    public AvailabilityRule saveRule(Long id, Long professionalId, DayOfWeek weekday, LocalTime start, LocalTime end,
                                     boolean active) {
        professionals.findById(professionalId).orElseThrow(ProfessionalNotFoundException::new);
        if (!start.isBefore(end)) throw new InvalidTimeRangeException();
        if (id != null) rules.findById(id).orElseThrow(RuleNotFoundException::new);
        return rules.save(new AvailabilityRule(id, professionalId, weekday, start, end, active));
    }

    @Transactional
    public void deleteRule(Long id) {
        rules.findById(id).orElseThrow(RuleNotFoundException::new);
        rules.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityException> exceptions(LocalDate from, LocalDate to) {
        return exceptions.findByDateRange(from, to);
    }

    @Transactional
    public AvailabilityException addException(Long professionalId, LocalDate date, LocalTime start, LocalTime end,
                                              ExceptionType type, String note) {
        if (professionalId != null) professionals.findById(professionalId).orElseThrow(ProfessionalNotFoundException::new);
        boolean wholeDay = start == null || end == null;
        if (wholeDay ? type != ExceptionType.BLOCK : !start.isBefore(end)) throw new InvalidTimeRangeException();
        return exceptions.save(new AvailabilityException(null, professionalId, date,
                wholeDay ? null : start, wholeDay ? null : end, type, note));
    }

    @Transactional
    public void deleteException(Long id) {
        exceptions.findById(id).orElseThrow(ExceptionNotFoundException::new);
        exceptions.deleteById(id);
    }
}
