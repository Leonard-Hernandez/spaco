package com.lsoftware.spaco.infrastructure.mcp;

import com.lsoftware.spaco.entity.availability.model.AvailabilityRule;
import com.lsoftware.spaco.entity.clinical.model.AccessChannel;
import com.lsoftware.spaco.entity.clinical.model.Actor;
import com.lsoftware.spaco.entity.exception.DomainException;
import com.lsoftware.spaco.entity.professional.model.Professional;
import com.lsoftware.spaco.entity.tenant.model.Tenant;
import com.lsoftware.spaco.entity.treatment.model.Treatment;
import com.lsoftware.spaco.entity.treatment.model.TreatmentCategory;
import com.lsoftware.spaco.usecase.availability.ManageScheduleUseCase;
import com.lsoftware.spaco.usecase.catalog.ManageCatalogUseCase;
import com.lsoftware.spaco.usecase.catalog.dto.TreatmentCommand;
import com.lsoftware.spaco.usecase.professional.ManageProfessionalUseCase;
import com.lsoftware.spaco.usecase.tenant.ManageTenantProfileUseCase;
import com.lsoftware.spaco.usecase.tenant.dto.TenantProfileCommand;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static com.lsoftware.spaco.usecase.Fixtures.BOGOTA;
import static com.lsoftware.spaco.usecase.Fixtures.SPA;
import static com.lsoftware.spaco.usecase.Fixtures.treatment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpaAdminToolsTest {

    private final ManageCatalogUseCase catalog = mock(ManageCatalogUseCase.class);
    private final ManageProfessionalUseCase professionals = mock(ManageProfessionalUseCase.class);
    private final ManageScheduleUseCase schedule = mock(ManageScheduleUseCase.class);
    private final ManageTenantProfileUseCase profile = mock(ManageTenantProfileUseCase.class);
    private final SpaAdminTools tools = new SpaAdminTools(catalog, professionals, schedule, profile,
            Validation.buildDefaultValidatorFactory().getValidator());
    private final Actor owner = new Actor("sub-owner", "Owner", AccessChannel.MCP);

    @Test
    void everyWriteToolIsMarkedAndTheReadOnesAreNot() {
        Map<String, Boolean> write = new java.util.LinkedHashMap<>();
        tools.tools().forEach(t -> write.put(t.name(), t.write()));

        assertThat(write).containsEntry("list_categories", false).containsEntry("list_availability_rules", false)
                .containsEntry("get_spa_profile", false)
                .containsEntry("create_category", true).containsEntry("update_category", true)
                .containsEntry("create_treatment", true).containsEntry("update_treatment", true)
                .containsEntry("create_professional", true).containsEntry("update_professional", true)
                .containsEntry("create_availability_rule", true).containsEntry("update_availability_rule", true)
                .containsEntry("update_spa_profile", true);
        assertThat(write).hasSize(12);
    }

    @Test
    void createTreatmentSavesWithTheCatalogueUseCase() {
        when(catalog.saveTreatment(eq(null), any())).thenReturn(treatment(9, 60, 10));

        call("create_treatment", Map.of("category_id", 1, "name", "Facial de miel", "duration_min", 60, "price", 150000,
                "contraindications", "Alergia a la miel"));

        ArgumentCaptor<TreatmentCommand> cmd = ArgumentCaptor.forClass(TreatmentCommand.class);
        verify(catalog).saveTreatment(eq(null), cmd.capture());
        assertThat(cmd.getValue()).satisfies(c -> {
            assertThat(c.categoryId()).isEqualTo(1L);
            assertThat(c.name()).isEqualTo("Facial de miel");
            assertThat(c.durationMin()).isEqualTo(60);
            assertThat(c.bufferMin()).isZero();
            assertThat(c.price()).isEqualTo(150000L);
            assertThat(c.active()).isTrue();
            assertThat(c.contraindications()).isEqualTo("Alergia a la miel");
        });
    }

    @Test
    void createTreatmentRejectsWhatThePanelRejects() {
        assertThatThrownBy(() -> call("create_treatment", Map.of("category_id", 1, "name", "x".repeat(121), "duration_min", 2)))
                .isInstanceOf(DomainException.class).hasMessageContaining("name").hasMessageContaining("durationMin");
        verify(catalog, never()).saveTreatment(any(), any());
    }

    @Test
    void updateTreatmentKeepsEveryFieldThatWasNotSent() {
        Treatment old = treatment(5, 45, 15);
        when(catalog.treatments()).thenReturn(List.of(old));
        when(catalog.saveTreatment(eq(5L), any())).thenReturn(old);

        call("update_treatment", Map.of("treatment_id", 5, "price", 99000, "active", false));

        ArgumentCaptor<TreatmentCommand> cmd = ArgumentCaptor.forClass(TreatmentCommand.class);
        verify(catalog).saveTreatment(eq(5L), cmd.capture());
        assertThat(cmd.getValue().price()).isEqualTo(99000L);
        assertThat(cmd.getValue().active()).isFalse();
        assertThat(cmd.getValue().name()).isEqualTo(old.name());
        assertThat(cmd.getValue().durationMin()).isEqualTo(45);
        assertThat(cmd.getValue().contraindications()).isEqualTo(old.contraindications());
    }

    @Test
    void updateTreatmentCanClearThePriceWithAnExplicitNull() {
        Treatment old = treatment(5, 45, 15);
        when(catalog.treatments()).thenReturn(List.of(old));
        when(catalog.saveTreatment(eq(5L), any())).thenReturn(old);
        Map<String, Object> args = new java.util.HashMap<>();
        args.put("treatment_id", 5);
        args.put("price", null);

        call("update_treatment", args);

        ArgumentCaptor<TreatmentCommand> cmd = ArgumentCaptor.forClass(TreatmentCommand.class);
        verify(catalog).saveTreatment(eq(5L), cmd.capture());
        assertThat(cmd.getValue().price()).isNull();
    }

    @Test
    void updatingATreatmentOfAnotherSpaIsNotFound() {
        when(catalog.treatments()).thenReturn(List.of(treatment(5, 45, 15)));

        assertThatThrownBy(() -> call("update_treatment", Map.of("treatment_id", 777, "active", false)))
                .isInstanceOf(DomainException.class).hasMessageContaining("not found");
        verify(catalog, never()).saveTreatment(any(), any());
    }

    @Test
    void updateWithoutAnyFieldIsRejected() {
        when(catalog.categories()).thenReturn(List.of(new TreatmentCategory(2L, "Faciales", "faciales", "honey", 0, true)));

        assertThatThrownBy(() -> call("update_category", Map.of("category_id", 2)))
                .isInstanceOf(DomainException.class).hasMessageContaining("Nothing to change");
    }

    @Test
    void categoryInkIsRestrictedToTheSiteColours() {
        assertThatThrownBy(() -> call("create_category", Map.of("name", "Spa", "ink", "pink")))
                .isInstanceOf(DomainException.class).hasMessageContaining("ink");
    }

    @Test
    void updateProfessionalKeepsTheLinkedAccount() {
        when(professionals.list()).thenReturn(List.of(new Professional(3L, "Ana", "Cosmetóloga", "Bio", "p.webp", 8L, true)));
        when(professionals.save(any(), any(), any(), any(), any(), eq(false)))
                .thenReturn(new Professional(3L, "Ana", "Cosmetóloga", "Bio", "p.webp", 8L, false));

        call("update_professional", Map.of("professional_id", 3, "active", false));

        verify(professionals).save(3L, "Ana", "Cosmetóloga", "Bio", 8L, false);
    }

    @Test
    void ruleTimesAreParsedAndMergedOnUpdate() {
        AvailabilityRule old = new AvailabilityRule(4L, 3L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0), true);
        when(schedule.rules()).thenReturn(List.of(old));
        when(schedule.saveRule(any(), any(), any(), any(), any(), eq(true))).thenReturn(old);

        call("update_availability_rule", Map.of("rule_id", 4, "end_time", "18:30"));

        verify(schedule).saveRule(4L, 3L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 30), true);
    }

    @Test
    void updateSpaProfileUsesTheProfileUseCaseAndKeepsOtherFields() {
        Tenant spa = new Tenant(1L, "miel-spa", "Miel Spa", BOGOTA, "+573001112233", null, 30, 120,
                Map.of("city", "Cali", "tagline", "Miel y calma"));
        when(profile.get()).thenReturn(spa);
        when(profile.update(any())).thenReturn(spa);

        call("update_spa_profile", Map.of("instagram", "@mielspa"));

        ArgumentCaptor<TenantProfileCommand> cmd = ArgumentCaptor.forClass(TenantProfileCommand.class);
        verify(profile).update(cmd.capture());
        assertThat(cmd.getValue().instagram()).isEqualTo("@mielspa");
        assertThat(cmd.getValue().city()).isEqualTo("Cali");
        assertThat(cmd.getValue().phone()).isEqualTo("+573001112233");
    }

    @Test
    void updateSpaProfileValidatesLikeThePanel() {
        when(profile.get()).thenReturn(SPA);

        assertThatThrownBy(() -> call("update_spa_profile", Map.of("email", "not-an-email", "phone", "abc")))
                .isInstanceOf(DomainException.class).hasMessageContaining("email").hasMessageContaining("phone");
        verify(profile, never()).update(any());
    }

    private Object call(String name, Map<String, Object> arguments) {
        McpTool tool = tools.tools().stream().filter(t -> t.name().equals(name)).findFirst().orElseThrow();
        return tool.handler().apply(new McpArgs(arguments), owner);
    }
}
