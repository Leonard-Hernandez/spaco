package com.lsoftware.spaco.usecase.tenant;

import com.lsoftware.spaco.entity.availability.gateway.AvailabilityRuleGateway;
import com.lsoftware.spaco.entity.availability.model.AvailabilityRule;
import com.lsoftware.spaco.entity.availability.model.OpeningDay;
import com.lsoftware.spaco.entity.professional.gateway.ProfessionalGateway;
import com.lsoftware.spaco.usecase.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetOpeningHoursUseCaseTest {

    private final AvailabilityRuleGateway rules = mock(AvailabilityRuleGateway.class);
    private final ProfessionalGateway professionals = mock(ProfessionalGateway.class);
    private final GetOpeningHoursUseCase useCase = new GetOpeningHoursUseCase(rules, professionals);

    @BeforeEach
    void setUp() {
        when(professionals.findAllActive()).thenReturn(List.of(Fixtures.professional(1L), Fixtures.professional(2L)));
    }

    @Test
    void rulesOfInactiveProfessionalsAreLeftOut() {
        when(rules.findAll()).thenReturn(List.of(
                rule(1L, DayOfWeek.MONDAY, "09:00", "12:00", true),
                rule(9L, DayOfWeek.MONDAY, "14:00", "20:00", true),
                rule(9L, DayOfWeek.SUNDAY, "09:00", "12:00", true)));

        assertThat(useCase.execute()).singleElement().satisfies(d -> {
            assertThat(d.weekday()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(d.blocks()).containsExactly(new OpeningDay.Block(LocalTime.of(9, 0), LocalTime.of(12, 0)));
        });
    }

    @Test
    void overlappingAndTouchingBlocksOfDifferentProfessionalsAreMerged() {
        when(rules.findAll()).thenReturn(List.of(
                rule(1L, DayOfWeek.MONDAY, "09:00", "13:00", true),
                rule(2L, DayOfWeek.MONDAY, "11:00", "15:00", true),
                rule(1L, DayOfWeek.MONDAY, "15:00", "18:00", true),
                rule(2L, DayOfWeek.MONDAY, "20:00", "21:00", true)));

        List<OpeningDay> days = useCase.execute();

        assertThat(days).hasSize(1);
        assertThat(days.getFirst().blocks()).containsExactly(
                new OpeningDay.Block(LocalTime.of(9, 0), LocalTime.of(18, 0)),
                new OpeningDay.Block(LocalTime.of(20, 0), LocalTime.of(21, 0)));
    }

    @Test
    void inactiveRulesAndClosedDaysAreLeftOut() {
        when(rules.findAll()).thenReturn(List.of(
                rule(1L, DayOfWeek.TUESDAY, "09:00", "12:00", false),
                rule(1L, DayOfWeek.SATURDAY, "08:00", "12:00", true),
                rule(1L, DayOfWeek.WEDNESDAY, "08:00", "12:00", true)));

        assertThat(useCase.execute()).extracting(OpeningDay::weekday)
                .containsExactly(DayOfWeek.WEDNESDAY, DayOfWeek.SATURDAY);
    }

    private static AvailabilityRule rule(long professional, DayOfWeek day, String start, String end, boolean active) {
        return new AvailabilityRule(null, professional, day, LocalTime.parse(start), LocalTime.parse(end), active);
    }
}
