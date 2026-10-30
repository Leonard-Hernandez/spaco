package com.lsoftware.spaco.infrastructure.mcp;

import com.lsoftware.spaco.entity.appointment.model.Appointment;
import com.lsoftware.spaco.entity.appointment.model.AppointmentStatus;
import com.lsoftware.spaco.entity.clinical.model.Actor;
import com.lsoftware.spaco.entity.clinical.model.SessionNote;
import com.lsoftware.spaco.entity.patient.model.Patient;
import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.entity.treatment.model.Treatment;
import com.lsoftware.spaco.usecase.appointment.ManageAppointmentUseCase;
import com.lsoftware.spaco.usecase.catalog.ManageCatalogUseCase;
import com.lsoftware.spaco.usecase.clinical.ClinicalRecordUseCase;
import com.lsoftware.spaco.usecase.clinical.PatientSummaryUseCase;
import com.lsoftware.spaco.usecase.clinical.SessionNoteUseCase;
import com.lsoftware.spaco.usecase.patient.ManagePatientUseCase;
import com.lsoftware.spaco.usecase.professional.ManageProfessionalUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only tools over the spa's data for the esthetician's assistant. They call the same use
 * cases as the panel, so every clinical read lands in the access log (channel MCP). Photos are
 * described, never sent.
 */
@Component
@RequiredArgsConstructor
public class SpaTools {

    private final ManagePatientUseCase patients;
    private final PatientSummaryUseCase summaries;
    private final ClinicalRecordUseCase records;
    private final SessionNoteUseCase sessions;
    private final ManageAppointmentUseCase appointments;
    private final ManageCatalogUseCase catalog;
    private final ManageProfessionalUseCase professionals;
    private final CurrentTenantGateway tenants;

    public List<McpTool> tools() {
        return List.of(
                new McpTool("search_patients", "Buscar pacientes",
                        "Find patients of the spa by name (accents and typos are tolerated), phone or email. "
                                + "Returns their ids; use it first whenever the user names a patient. Empty query = most recent.",
                        Schemas.object().string("query", "Name, part of the name, phone or email", false).build(),
                        (a, actor) -> patients.search(a.string("query")).stream().map(SpaTools::patient).toList()),
                new McpTool("get_patient_summary", "Resumen de la paciente",
                        "Everything to know before treating a patient: contact, age, anamnesis (allergies, medications, "
                                + "conditions, pregnancy, skin type, phototype), alerts, last 3 sessions, upcoming appointments and valid consents.",
                        Schemas.object().integer("patient_id", "Patient id (from search_patients)", true).build(),
                        (a, actor) -> summaries.summary(a.requiredId("patient_id"), actor)),
                new McpTool("get_clinical_record", "Anamnesis",
                        "The anamnesis of a patient as last recorded, with who updated it and when.",
                        Schemas.object().integer("patient_id", "Patient id", true).build(),
                        (a, actor) -> records.get(a.requiredId("patient_id"), actor)),
                new McpTool("list_sessions", "Sesiones de la paciente",
                        "Treatment sessions of a patient, newest first: treatment, professional, products, device "
                                + "parameters, observations, recommendations, and which before/after photos exist.",
                        Schemas.object()
                                .integer("patient_id", "Patient id", true)
                                .date("from", "Only sessions on or after this date (YYYY-MM-DD)", false)
                                .date("to", "Only sessions before this date (YYYY-MM-DD)", false)
                                .build(),
                        (a, actor) -> sessions.list(a.requiredId("patient_id"), start(a.date("from")), start(a.date("to")), actor)
                                .stream().map(SpaTools::session).toList()),
                new McpTool("get_session", "Detalle de una sesión",
                        "One session note by id.",
                        Schemas.object().integer("session_id", "Session id", true).build(),
                        (a, actor) -> session(sessions.get(a.requiredId("session_id"), actor))),
                new McpTool("search_session_notes", "Buscar en las notas",
                        "Full-text search (Spanish) over every session note: products, parameters, observations and "
                                + "recommendations. E.g. 'enrojecimiento', 'ácido hialurónico', \"peeling -químico\". Optionally one patient.",
                        Schemas.object()
                                .string("text", "Words to look for (web-search syntax: quotes, OR, -word)", true)
                                .integer("patient_id", "Only this patient's notes", false)
                                .build(),
                        (a, actor) -> summaries.searchNotes(a.requiredString("text"), a.id("patient_id"), actor)),
                new McpTool("check_contraindications", "Revisar contraindicaciones",
                        "Puts a treatment's contraindications next to the patient's history and lists the words that "
                                + "match. A reminder for the professional, not a medical decision.",
                        Schemas.object()
                                .integer("patient_id", "Patient id", true)
                                .integer("treatment_id", "Treatment id (from list_treatments)", true)
                                .build(),
                        (a, actor) -> summaries.checkContraindications(a.requiredId("patient_id"), a.requiredId("treatment_id"), actor)),
                new McpTool("get_patient_appointments", "Citas de la paciente",
                        "All appointments of a patient (past and upcoming) with their status.",
                        Schemas.object().integer("patient_id", "Patient id", true).build(),
                        (a, actor) -> appointments.ofPatient(a.requiredId("patient_id")).stream().map(this::appointment).toList()),
                new McpTool("list_appointments", "Agenda",
                        "Appointments of the spa between two dates (spa local time), optionally of one professional or status.",
                        Schemas.object()
                                .date("from", "First day (YYYY-MM-DD)", true)
                                .date("to", "Last day, inclusive (YYYY-MM-DD). Default: same as from", false)
                                .integer("professional_id", "Only this professional", false)
                                .enumeration("status", "Only this status",
                                        Arrays.stream(AppointmentStatus.values()).map(Enum::name).toList(), false)
                                .build(),
                        (a, actor) -> {
                            LocalDate from = a.date("from");
                            if (from == null) from = LocalDate.now(zone());
                            LocalDate to = a.date("to") != null ? a.date("to") : from;
                            String status = a.string("status");
                            return appointments.list(start(from), start(to.plusDays(1)),
                                            status == null ? null : AppointmentStatus.valueOf(status), a.id("professional_id"))
                                    .stream().map(this::appointment).toList();
                        }),
                new McpTool("list_treatments", "Tratamientos",
                        "The spa's treatments: id, category, duration, price (null = not published), description and contraindications.",
                        Schemas.object().build(),
                        (a, actor) -> catalog.treatments().stream().map(SpaTools::treatment).toList()),
                new McpTool("list_professionals", "Profesionales",
                        "The spa's professionals with their ids.",
                        Schemas.object().build(),
                        (a, actor) -> professionals.list().stream()
                                .map(p -> Map.of("id", p.id(), "name", p.displayName(), "active", p.active()))
                                .toList()));
    }

    private ZoneId zone() {
        return tenants.current().timezone();
    }

    private OffsetDateTime start(LocalDate date) {
        return date == null ? null : date.atStartOfDay(zone()).toOffsetDateTime();
    }

    private static Map<String, Object> patient(Patient p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.id());
        m.put("name", p.fullName());
        m.put("phone", p.phoneE164());
        m.put("email", p.email());
        m.put("birthDate", p.birthDate());
        m.put("since", p.createdAt());
        return m;
    }

    private static Map<String, Object> session(SessionNote n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.id());
        m.put("patientId", n.patientId());
        m.put("performedAt", n.performedAt());
        m.put("treatment", n.treatmentName());
        m.put("professional", n.professionalName());
        m.put("products", n.products());
        m.put("parameters", n.parameters());
        m.put("observations", n.observations());
        m.put("recommendations", n.recommendations());
        m.put("photos", n.photos().stream()
                .map(p -> Map.of("kind", p.kind().name(), "caption", p.caption() == null ? "" : p.caption()))
                .toList());
        return m;
    }

    private Map<String, Object> appointment(Appointment a) {
        ZoneId tz = zone();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.id());
        m.put("start", a.startAt().atZoneSameInstant(tz).toLocalDateTime());
        m.put("end", a.endAt().atZoneSameInstant(tz).toLocalDateTime());
        m.put("status", a.status().name());
        m.put("treatment", a.treatmentName());
        m.put("professional", a.professionalName());
        m.put("patientId", a.patientId());
        m.put("patient", a.patientName());
        m.put("patientNote", a.patientNote());
        return m;
    }

    private static Map<String, Object> treatment(Treatment t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.id());
        m.put("name", t.name());
        m.put("category", t.categoryName());
        m.put("durationMin", t.durationMin());
        m.put("price", t.price());
        m.put("active", t.active());
        m.put("tagline", t.tagline());
        m.put("description", t.description());
        m.put("contraindications", t.contraindications());
        return m;
    }
}
