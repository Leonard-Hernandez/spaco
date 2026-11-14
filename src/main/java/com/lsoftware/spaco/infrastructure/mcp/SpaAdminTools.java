package com.lsoftware.spaco.infrastructure.mcp;

import com.lsoftware.spaco.entity.availability.exception.RuleNotFoundException;
import com.lsoftware.spaco.entity.availability.model.AvailabilityRule;
import com.lsoftware.spaco.entity.exception.DomainException;
import com.lsoftware.spaco.entity.professional.exception.ProfessionalNotFoundException;
import com.lsoftware.spaco.entity.professional.model.Professional;
import com.lsoftware.spaco.entity.treatment.exception.CategoryNotFoundException;
import com.lsoftware.spaco.entity.treatment.exception.TreatmentNotFoundException;
import com.lsoftware.spaco.entity.treatment.model.Treatment;
import com.lsoftware.spaco.entity.treatment.model.TreatmentCategory;
import com.lsoftware.spaco.infrastructure.availability.dto.RuleRequest;
import com.lsoftware.spaco.infrastructure.catalog.dto.CategoryRequest;
import com.lsoftware.spaco.infrastructure.catalog.dto.TreatmentRequest;
import com.lsoftware.spaco.infrastructure.professional.dto.ProfessionalRequest;
import com.lsoftware.spaco.infrastructure.tenant.dto.TenantProfileRequest;
import com.lsoftware.spaco.infrastructure.tenant.dto.TenantProfileResponse;
import com.lsoftware.spaco.usecase.availability.ManageScheduleUseCase;
import com.lsoftware.spaco.usecase.catalog.ManageCatalogUseCase;
import com.lsoftware.spaco.usecase.professional.ManageProfessionalUseCase;
import com.lsoftware.spaco.usecase.tenant.ManageTenantProfileUseCase;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tools to run the spa from the assistant: catalogue, team, weekly hours and public profile. They
 * call the same use cases as the owner's panel and validate with the same constraints as its
 * request DTOs. Write tools are owner-only (the controller enforces it and audits each call).
 * There is no hard delete: records are switched off with active=false. Updates are partial:
 * omitted fields keep their value, an explicit null clears the nullable ones.
 * Nothing here touches patients, clinical records, session notes or appointments.
 */
@Component
@RequiredArgsConstructor
public class SpaAdminTools {

    private static final List<String> INKS = List.of("honey", "lavender", "sage", "navy", "clay");
    private static final List<String> WEEKDAYS = Arrays.stream(DayOfWeek.values()).map(Enum::name).toList();

    private final ManageCatalogUseCase catalog;
    private final ManageProfessionalUseCase professionals;
    private final ManageScheduleUseCase schedule;
    private final ManageTenantProfileUseCase profile;
    private final Validator validator;

    public List<McpTool> tools() {
        return List.of(
                new McpTool("list_categories", "Categorías",
                        "The treatment categories of the spa: id, name, ink (label colour), position and active.",
                        Schemas.object().build(),
                        (a, actor) -> catalog.categories()),
                McpTool.create("create_category", "Crear categoría",
                        "Create a treatment category. The name must be unique (case and accents ignored).",
                        categorySchema(true),
                        (a, actor) -> {
                            CategoryRequest req = validated(new CategoryRequest(a.requiredString("name"),
                                    a.requiredString("ink"), a.intValue("position", 0), a.bool("active", true)));
                            return catalog.saveCategory(null, req.name(), req.ink(), req.position(), req.active());
                        }),
                McpTool.update("update_category", "Editar categoría",
                        "Change a category. Only the fields you send change. Use active=false to hide it instead of deleting.",
                        categorySchema(false),
                        (a, actor) -> {
                            Long id = a.requiredId("category_id");
                            TreatmentCategory old = catalog.categories().stream().filter(c -> id.equals(c.id())).findFirst()
                                    .orElseThrow(CategoryNotFoundException::new);
                            requireChange(a, "name", "ink", "position", "active");
                            CategoryRequest req = validated(new CategoryRequest(a.text("name", old.name()),
                                    a.text("ink", old.ink()), a.intValue("position", old.position()),
                                    a.bool("active", old.active())));
                            return catalog.saveCategory(id, req.name(), req.ink(), req.position(), req.active());
                        }),
                McpTool.create("create_treatment", "Crear tratamiento",
                        "Create a treatment in a category (ids from list_categories). Price is in whole pesos; omit it "
                                + "while it is not published. contraindications is what check_contraindications compares to the patient's history.",
                        treatmentSchema(true),
                        (a, actor) -> {
                            TreatmentRequest req = validated(new TreatmentRequest(a.requiredId("category_id"), null,
                                    a.requiredString("name"), a.string("tagline"), a.string("description"),
                                    a.stringList("benefits", List.of()), List.of(), a.string("contraindications"),
                                    Math.toIntExact(a.requiredId("duration_min")), a.intValue("buffer_min", 0),
                                    a.id("price"), a.intValue("position", 0), a.bool("active", true)));
                            return SpaTools.treatment(catalog.saveTreatment(null, req.toCommand()));
                        }),
                McpTool.update("update_treatment", "Editar tratamiento",
                        "Change a treatment. Only the fields you send change; null clears tagline, description, "
                                + "contraindications or price. Use active=false to take it off the public site and the agenda.",
                        treatmentSchema(false),
                        (a, actor) -> {
                            Long id = a.requiredId("treatment_id");
                            Treatment old = catalog.treatments().stream().filter(t -> id.equals(t.id())).findFirst()
                                    .orElseThrow(TreatmentNotFoundException::new);
                            requireChange(a, "category_id", "name", "tagline", "description", "contraindications",
                                    "duration_min", "buffer_min", "price", "position", "active", "benefits");
                            TreatmentRequest req = validated(new TreatmentRequest(
                                    a.number("category_id", old.categoryId()), old.formulaNo(),
                                    a.text("name", old.name()), a.text("tagline", old.tagline()),
                                    a.text("description", old.description()), a.stringList("benefits", old.benefits()),
                                    old.composition(), a.text("contraindications", old.contraindications()),
                                    a.intValue("duration_min", old.durationMin()), a.intValue("buffer_min", old.bufferMin()),
                                    a.number("price", old.price()), a.intValue("position", old.position()),
                                    a.bool("active", old.active())));
                            return SpaTools.treatment(catalog.saveTreatment(id, req.toCommand()));
                        }),
                McpTool.create("create_professional", "Crear profesional",
                        "Add a person to the team. Photos are uploaded from the panel, not here.",
                        professionalSchema(true),
                        (a, actor) -> {
                            ProfessionalRequest req = validated(new ProfessionalRequest(a.requiredString("display_name"),
                                    a.string("title"), a.string("bio"), null, a.bool("active", true)));
                            return professional(professionals.save(null, req.displayName(), req.title(), req.bio(),
                                    req.memberId(), req.active()));
                        }),
                McpTool.update("update_professional", "Editar profesional",
                        "Change a team member. Only the fields you send change; null clears title or bio. "
                                + "Use active=false to retire them. The photo and the linked panel account are kept.",
                        professionalSchema(false),
                        (a, actor) -> {
                            Long id = a.requiredId("professional_id");
                            Professional old = professionals.list().stream().filter(p -> id.equals(p.id())).findFirst()
                                    .orElseThrow(ProfessionalNotFoundException::new);
                            requireChange(a, "display_name", "title", "bio", "active");
                            ProfessionalRequest req = validated(new ProfessionalRequest(
                                    a.text("display_name", old.displayName()), a.text("title", old.title()),
                                    a.text("bio", old.bio()), old.memberId(), a.bool("active", old.active())));
                            return professional(professionals.save(id, req.displayName(), req.title(), req.bio(),
                                    req.memberId(), req.active()));
                        }),
                new McpTool("list_availability_rules", "Horarios semanales",
                        "The weekly working hours: one rule per professional, weekday and time range, with id and active.",
                        Schemas.object().build(),
                        (a, actor) -> schedule.rules()),
                McpTool.create("create_availability_rule", "Crear horario",
                        "Add a weekly working block for a professional (ids from list_professionals). End must be after start.",
                        ruleSchema(true),
                        (a, actor) -> {
                            RuleRequest req = validated(new RuleRequest(a.requiredId("professional_id"),
                                    weekday(a.requiredString("weekday")), a.time("start_time"), a.time("end_time"),
                                    a.bool("active", true)));
                            return schedule.saveRule(null, req.professionalId(), req.weekday(), req.startTime(),
                                    req.endTime(), req.active());
                        }),
                McpTool.update("update_availability_rule", "Editar horario",
                        "Change a weekly working block. Only the fields you send change. Use active=false to switch the block off.",
                        ruleSchema(false),
                        (a, actor) -> {
                            Long id = a.requiredId("rule_id");
                            AvailabilityRule old = schedule.rules().stream().filter(r -> id.equals(r.id())).findFirst()
                                    .orElseThrow(RuleNotFoundException::new);
                            requireChange(a, "professional_id", "weekday", "start_time", "end_time", "active");
                            RuleRequest req = validated(new RuleRequest(a.number("professional_id", old.professionalId()),
                                    a.has("weekday") ? weekday(a.requiredString("weekday")) : old.weekday(),
                                    a.has("start_time") ? a.time("start_time") : old.startTime(),
                                    a.has("end_time") ? a.time("end_time") : old.endTime(),
                                    a.bool("active", old.active())));
                            return schedule.saveRule(id, req.professionalId(), req.weekday(), req.startTime(),
                                    req.endTime(), req.active());
                        }),
                new McpTool("get_spa_profile", "Perfil del spa",
                        "The public profile of the spa: phone, email, address, city, tagline, intro, instagram and cancellation note.",
                        Schemas.object().build(),
                        (a, actor) -> TenantProfileResponse.from(profile.get())),
                McpTool.update("update_spa_profile", "Editar perfil del spa",
                        "Change the public profile shown on the site. Only the fields you send change; null or empty clears one. "
                                + "Opening hours are not here: they come from the weekly rules.",
                        Schemas.object()
                                .string("phone", "Phone / WhatsApp, digits with optional + ( ) - . and spaces", false, 30, true)
                                .string("email", "Contact email", false, 120, true)
                                .string("address", "Street address", false, 255, true)
                                .string("city", "City", false, 80, true)
                                .string("tagline", "Short line under the name", false, 160, true)
                                .string("intro", "Presentation paragraph", false, 600, true)
                                .string("instagram", "Instagram handle, with or without @", false, 60, true)
                                .string("cancellation_note", "Cancellation policy shown when booking", false, 300, true)
                                .build(),
                        (a, actor) -> {
                            requireChange(a, "phone", "email", "address", "city", "tagline", "intro", "instagram",
                                    "cancellation_note");
                            TenantProfileResponse old = TenantProfileResponse.from(profile.get());
                            TenantProfileRequest req = validated(new TenantProfileRequest(
                                    a.text("phone", old.phone()), a.text("email", old.email()),
                                    a.text("address", old.address()), a.text("city", old.city()),
                                    a.text("tagline", old.tagline()), a.text("intro", old.intro()),
                                    a.text("instagram", old.instagram()),
                                    a.text("cancellation_note", old.cancellationNote())));
                            return TenantProfileResponse.from(profile.update(req.toCommand()));
                        }));
    }

    private static Map<String, Object> categorySchema(boolean create) {
        Schemas s = Schemas.object();
        if (!create) s.integer("category_id", "Category id (from list_categories)", true);
        return s.string("name", "Category name", create, 80, false)
                .enumeration("ink", "Label colour on the site", INKS, create)
                .integer("position", "Sort order, lowest first (default 0)", false, 0, 10_000L, false)
                .bool("active", "Visible on the site (default true)")
                .build();
    }

    private static Map<String, Object> treatmentSchema(boolean create) {
        Schemas s = Schemas.object();
        if (!create) s.integer("treatment_id", "Treatment id (from list_treatments)", true);
        return s.integer("category_id", "Category id (from list_categories)", create, 1, null, false)
                .string("name", "Treatment name (unique)", create, 120, false)
                .string("tagline", "One-line pitch", false, 200, true)
                .string("description", "Full description", false, 4000, true)
                .stringList("benefits", "Benefit bullets (replaces the list)", 12, 160)
                .string("contraindications", "Contraindications, compared with the patient's history", false, 4000, true)
                .integer("duration_min", "Minutes the treatment takes (5-600)", create, 5, 600L, false)
                .integer("buffer_min", "Minutes to prepare the room afterwards (0-120, default 0)", false, 0, 120L, false)
                .integer("price", "Price in whole pesos; null = not published", false, 0, null, true)
                .integer("position", "Sort order, lowest first (default 0)", false, 0, 10_000L, false)
                .bool("active", "Offered to patients (default true)")
                .build();
    }

    private static Map<String, Object> professionalSchema(boolean create) {
        Schemas s = Schemas.object();
        if (!create) s.integer("professional_id", "Professional id (from list_professionals)", true);
        return s.string("display_name", "Name shown to patients", create, 120, false)
                .string("title", "Role, e.g. Cosmetóloga", false, 120, true)
                .string("bio", "Short biography", false, 2000, true)
                .bool("active", "Takes appointments (default true)")
                .build();
    }

    private static Map<String, Object> ruleSchema(boolean create) {
        Schemas s = Schemas.object();
        if (!create) s.integer("rule_id", "Rule id (from list_availability_rules)", true);
        return s.integer("professional_id", "Professional id (from list_professionals)", create, 1, null, false)
                .enumeration("weekday", "Day of the week", WEEKDAYS, create)
                .time("start_time", "Start of the block", create)
                .time("end_time", "End of the block, after start_time", create)
                .bool("active", "Block is in use (default true)")
                .build();
    }

    private static Map<String, Object> professional(Professional p) {
        return Map.of("id", p.id(), "name", p.displayName(), "title", p.title() == null ? "" : p.title(),
                "bio", p.bio() == null ? "" : p.bio(), "active", p.active());
    }

    private static DayOfWeek weekday(String value) {
        try {
            return DayOfWeek.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new DomainException("INVALID_ARGUMENT", "weekday must be one of " + WEEKDAYS);
        }
    }

    private static void requireChange(McpArgs a, String... fields) {
        if (Arrays.stream(fields).noneMatch(a::has)) {
            throw new DomainException("INVALID_ARGUMENT", "Nothing to change: send at least one of " + String.join(", ", fields));
        }
    }

    /** Same constraints as the panel's request DTOs. */
    private <T> T validated(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new DomainException("INVALID_ARGUMENT", violations.stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage()).sorted().collect(Collectors.joining("; ")));
        }
        return request;
    }
}
