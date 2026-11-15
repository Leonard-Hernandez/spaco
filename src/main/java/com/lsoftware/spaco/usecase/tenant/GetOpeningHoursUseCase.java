package com.lsoftware.spaco.usecase.tenant;

import com.lsoftware.spaco.entity.availability.gateway.AvailabilityRuleGateway;
import com.lsoftware.spaco.entity.availability.model.AvailabilityRule;
import com.lsoftware.spaco.entity.availability.model.OpeningDay;
import com.lsoftware.spaco.entity.professional.gateway.ProfessionalGateway;
import com.lsoftware.spaco.entity.professional.model.Professional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Public opening hours: the active weekly rules of every active professional, merged per weekday. */
@Component
@RequiredArgsConstructor
public class GetOpeningHoursUseCase {

    private final AvailabilityRuleGateway rules;
    private final ProfessionalGateway professionals;

    /** One entry per weekday that has at least one active block, Monday first. */
    @Transactional(readOnly = true)
    public List<OpeningDay> execute() {
        Set<Long> activeProfessionals = professionals.findAllActive().stream()
                .map(Professional::id).collect(Collectors.toSet());
        List<AvailabilityRule> active = rules.findAll().stream()
                .filter(AvailabilityRule::active)
                .filter(r -> activeProfessionals.contains(r.professionalId()))
                .toList();
        List<OpeningDay> days = new ArrayList<>();
        for (DayOfWeek weekday : DayOfWeek.values()) {
            List<OpeningDay.Block> blocks = merge(active.stream()
                    .filter(r -> r.weekday() == weekday)
                    .map(r -> new OpeningDay.Block(r.startTime(), r.endTime()))
                    .toList());
            if (!blocks.isEmpty()) days.add(new OpeningDay(weekday, blocks));
        }
        return days;
    }

    /** Sorts the blocks and joins those that overlap or touch. */
    static List<OpeningDay.Block> merge(List<OpeningDay.Block> blocks) {
        List<OpeningDay.Block> sorted = blocks.stream().sorted(Comparator.comparing(OpeningDay.Block::start)).toList();
        List<OpeningDay.Block> merged = new ArrayList<>();
        for (OpeningDay.Block b : sorted) {
            OpeningDay.Block last = merged.isEmpty() ? null : merged.getLast();
            if (last != null && !b.start().isAfter(last.end())) {
                if (b.end().isAfter(last.end())) merged.set(merged.size() - 1, new OpeningDay.Block(last.start(), b.end()));
            } else {
                merged.add(b);
            }
        }
        return merged;
    }
}
