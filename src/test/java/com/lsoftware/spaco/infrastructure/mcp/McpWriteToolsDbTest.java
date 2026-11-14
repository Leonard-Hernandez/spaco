package com.lsoftware.spaco.infrastructure.mcp;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The MCP write tools over HTTP against a real (throwaway) Postgres: JSON-RPC in, Hibernate
 * tenant filter, validation and the access log all included. It only runs when SPACO_IT_DB is set
 * (the DB_URL / USER_DB / PASSWORD_DB env vars then point at the database), e.g.
 * <pre>docker run -d -e POSTGRES_PASSWORD=pw -e POSTGRES_DB=spaco -p 55432:5432 postgres:17
 * SPACO_IT_DB=1 DB_URL=jdbc:postgresql://localhost:55432/spaco USER_DB=postgres PASSWORD_DB=pw ./gradlew test</pre>
 * With SPACO_PROOF_FILE set it also writes every request / response pair there as markdown.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "SPACO_IT_DB", matches = ".+")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class McpWriteToolsDbTest {

    private static final String OWNER = "sub-owner";
    private static final String ESTHETICIAN = "sub-esthetician";
    private static final List<String> TRANSCRIPT = new ArrayList<>();

    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;

    private MockMvc mvc;
    private static long otherTreatmentId;
    private static long otherCategoryId;
    private static long otherProfessionalId;
    private static long otherRuleId;
    private static long mielCategoryId;
    private static long createdTreatmentId;

    @BeforeAll
    static void clearTranscript() {
        TRANSCRIPT.clear();
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
        if (mielCategoryId != 0) return;
        long miel = jdbc.queryForObject("SELECT id FROM tenant WHERE slug = 'miel-spa'", Long.class);
        mielCategoryId = jdbc.queryForObject("SELECT min(id) FROM treatment_category WHERE tenant_id = ?", Long.class, miel);
        jdbc.update("INSERT INTO tenant_member (tenant_id, email, authco_sub, name, role) VALUES (?, 'owner@example.com', ?, 'Owner Example', 'OWNER')", miel, OWNER);
        jdbc.update("INSERT INTO tenant_member (tenant_id, email, authco_sub, name, role) VALUES (?, 'esthetician@example.com', ?, 'Esthetician Example', 'ESTHETICIAN')", miel, ESTHETICIAN);
        // A second spa whose rows the first spa's owner must never reach
        long other = jdbc.queryForObject("INSERT INTO tenant (slug, name) VALUES ('otro-spa', 'Otro Spa') RETURNING id", Long.class);
        otherCategoryId = jdbc.queryForObject("INSERT INTO treatment_category (tenant_id, name, slug) VALUES (?, 'Otra', 'otra') RETURNING id", Long.class, other);
        otherTreatmentId = jdbc.queryForObject("INSERT INTO treatment (tenant_id, category_id, name, slug, duration_min) VALUES (?, ?, 'Secreto', 'secreto', 30) RETURNING id", Long.class, other, otherCategoryId);
        otherProfessionalId = jdbc.queryForObject("INSERT INTO professional (tenant_id, display_name) VALUES (?, 'Otra Persona') RETURNING id", Long.class, other);
        otherRuleId = jdbc.queryForObject("INSERT INTO availability_rule (tenant_id, professional_id, weekday, start_time, end_time) VALUES (?, ?, 1, '09:00', '12:00') RETURNING id", Long.class, other, otherProfessionalId);
    }

    @Test
    @Order(1)
    void ownerSeesWriteToolsWithAnnotations() throws Exception {
        JsonNode res = rpc(OWNER, "Owner: list the tools", "tools/list", Map.of());

        List<String> names = res.at("/result/tools").valueStream().map(t -> t.get("name").asString()).toList();
        assertThat(names).contains("search_patients", "create_treatment", "update_treatment", "update_spa_profile",
                "create_category", "update_category", "create_professional", "update_professional",
                "list_availability_rules", "create_availability_rule", "update_availability_rule", "get_spa_profile");
        JsonNode create = res.at("/result/tools").valueStream().filter(t -> t.get("name").asString().equals("create_treatment")).findFirst().orElseThrow();
        assertThat(create.at("/annotations/readOnlyHint").asBoolean(true)).isFalse();
        assertThat(create.at("/annotations/destructiveHint").asBoolean(true)).isFalse();
    }

    @Test
    @Order(2)
    void estheticianOnlySeesReadTools() throws Exception {
        JsonNode res = rpc(ESTHETICIAN, "Esthetician: list the tools", "tools/list", Map.of());

        List<String> names = res.at("/result/tools").valueStream().map(t -> t.get("name").asString()).toList();
        assertThat(names).contains("list_treatments", "get_spa_profile", "list_categories", "list_availability_rules")
                .doesNotContain("create_treatment", "update_spa_profile", "update_category");
    }

    @Test
    @Order(3)
    void ownerCreatesATreatment() throws Exception {
        JsonNode res = call(OWNER, "Owner: create_treatment", "create_treatment", Map.of(
                "category_id", mielCategoryId, "name", "Facial de prueba", "tagline", "Un facial para la prueba",
                "duration_min", 50, "buffer_min", 10, "price", 180000,
                "contraindications", "Embarazo; alergia a la miel"));

        assertThat(res.at("/result/isError").asBoolean()).isFalse();
        JsonNode created = json.readTree(res.at("/result/content/0/text").asString());
        createdTreatmentId = created.get("id").asLong();
        assertThat(created.get("name").asString()).isEqualTo("Facial de prueba");
        assertThat(jdbc.queryForObject("SELECT price FROM treatment WHERE id = ?", Long.class, createdTreatmentId)).isEqualTo(180000L);

        JsonNode update = call(OWNER, "Owner: update_treatment (partial: only price and active)", "update_treatment",
                Map.of("treatment_id", createdTreatmentId, "price", 200000, "active", false));
        assertThat(update.at("/result/isError").asBoolean()).isFalse();
        assertThat(jdbc.queryForMap("SELECT price, active, duration_min, name FROM treatment WHERE id = ?", createdTreatmentId))
                .containsEntry("price", 200000L).containsEntry("active", false).containsEntry("duration_min", 50)
                .containsEntry("name", "Facial de prueba");
    }

    @Test
    @Order(4)
    void ownerUpdatesTheSpaProfile() throws Exception {
        JsonNode res = call(OWNER, "Owner: update_spa_profile", "update_spa_profile", Map.of(
                "phone", "+57 300 111 2233", "email", "hola@example.com", "city", "Cali",
                "tagline", "Miel y calma", "instagram", "mielspa"));

        assertThat(res.at("/result/isError").asBoolean()).isFalse();
        JsonNode profile = json.readTree(res.at("/result/content/0/text").asString());
        assertThat(profile.get("instagram").asString()).isEqualTo("@mielspa");
        assertThat(profile.get("city").asString()).isEqualTo("Cali");

        JsonNode read = call(ESTHETICIAN, "Esthetician: get_spa_profile (reads stay open)", "get_spa_profile", Map.of());
        assertThat(read.at("/result/isError").asBoolean()).isFalse();
        assertThat(read.at("/result/content/0/text").asString()).contains("hola@example.com");
    }

    @Test
    @Order(5)
    void validationErrorsComeBackAsToolErrors() throws Exception {
        JsonNode schema = call(OWNER, "Owner: create_treatment with invalid arguments", "create_treatment", Map.of(
                "category_id", mielCategoryId, "name", "Sin duración", "duration_min", 2, "color", "rojo"));
        assertThat(schema.at("/result/isError").asBoolean()).isTrue();
        assertThat(schema.at("/result/content/0/text").asString()).contains("INVALID_ARGUMENT")
                .contains("duration_min must be at least 5").contains("unknown argument: color");

        JsonNode bean = call(OWNER, "Owner: update_spa_profile with a bad email", "update_spa_profile",
                Map.of("email", "no-es-un-correo"));
        assertThat(bean.at("/result/isError").asBoolean()).isTrue();
        assertThat(bean.at("/result/content/0/text").asString()).contains("INVALID_ARGUMENT").contains("email");
    }

    @Test
    @Order(6)
    void anEstheticianCannotWrite() throws Exception {
        long before = jdbc.queryForObject("SELECT count(*) FROM treatment", Long.class);

        JsonNode res = call(ESTHETICIAN, "Esthetician: create_treatment (forbidden)", "create_treatment", Map.of(
                "category_id", mielCategoryId, "name", "No debería existir", "duration_min", 30));

        assertThat(res.at("/result/isError").asBoolean()).isTrue();
        assertThat(res.at("/result/content/0/text").asString()).startsWith("FORBIDDEN");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM treatment", Long.class)).isEqualTo(before);
    }

    @Test
    @Order(7)
    void anotherSpasIdsAreNotReachable() throws Exception {
        assertNotFound(call(OWNER, "Owner of miel-spa: update_treatment with a treatment of another spa", "update_treatment",
                Map.of("treatment_id", otherTreatmentId, "name", "Hackeado")), "TREATMENT_NOT_FOUND");
        assertNotFound(call(OWNER, "Owner of miel-spa: create_treatment in a category of another spa", "create_treatment",
                Map.of("category_id", otherCategoryId, "name", "Intruso", "duration_min", 30)), "CATEGORY_NOT_FOUND");
        assertNotFound(call(OWNER, "Owner of miel-spa: update_category of another spa", "update_category",
                Map.of("category_id", otherCategoryId, "name", "Hackeada")), "CATEGORY_NOT_FOUND");
        assertNotFound(call(OWNER, "Owner of miel-spa: update_professional of another spa", "update_professional",
                Map.of("professional_id", otherProfessionalId, "active", false)), "PROFESSIONAL_NOT_FOUND");
        assertNotFound(call(OWNER, "Owner of miel-spa: update_availability_rule of another spa", "update_availability_rule",
                Map.of("rule_id", otherRuleId, "active", false)), "RULE_NOT_FOUND");
        assertNotFound(call(OWNER, "Owner of miel-spa: create_availability_rule for a professional of another spa",
                "create_availability_rule", Map.of("professional_id", otherProfessionalId, "weekday", "TUESDAY",
                        "start_time", "09:00", "end_time", "10:00")), "PROFESSIONAL_NOT_FOUND");

        assertThat(jdbc.queryForObject("SELECT name FROM treatment WHERE id = ?", String.class, otherTreatmentId)).isEqualTo("Secreto");
        assertThat(jdbc.queryForObject("SELECT name FROM treatment_category WHERE id = ?", String.class, otherCategoryId)).isEqualTo("Otra");
        assertThat(jdbc.queryForObject("SELECT active FROM professional WHERE id = ?", Boolean.class, otherProfessionalId)).isTrue();
        assertThat(jdbc.queryForObject("SELECT active FROM availability_rule WHERE id = ?", Boolean.class, otherRuleId)).isTrue();
    }

    @Test
    @Order(8)
    void teamAndHoursToolsWork() throws Exception {
        JsonNode pro = call(OWNER, "Owner: create_professional", "create_professional",
                Map.of("display_name", "Profesional de prueba", "title", "Cosmetóloga"));
        long proId = json.readTree(pro.at("/result/content/0/text").asString()).get("id").asLong();
        JsonNode rule = call(OWNER, "Owner: create_availability_rule", "create_availability_rule", Map.of(
                "professional_id", proId, "weekday", "SATURDAY", "start_time", "09:00", "end_time", "13:30"));
        assertThat(rule.at("/result/isError").asBoolean()).isFalse();
        long ruleId = json.readTree(rule.at("/result/content/0/text").asString()).get("id").asLong();
        JsonNode off = call(OWNER, "Owner: update_availability_rule (deactivate)", "update_availability_rule",
                Map.of("rule_id", ruleId, "active", false));
        assertThat(off.at("/result/isError").asBoolean()).isFalse();
        assertThat(jdbc.queryForObject("SELECT active FROM availability_rule WHERE id = ?", Boolean.class, ruleId)).isFalse();
        JsonNode bad = call(OWNER, "Owner: update_availability_rule with end before start", "update_availability_rule",
                Map.of("rule_id", ruleId, "end_time", "08:00"));
        assertThat(bad.at("/result/isError").asBoolean()).isTrue();
    }

    @Test
    @Order(9)
    void everyWriteAttemptIsInTheAccessLogWithChannelMcp() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT actor_name, channel, action FROM clinical_access_log WHERE action ~ '^(write|denied|failed):' ORDER BY id");
        TRANSCRIPT.add("### Access log rows written by the calls above (`clinical_access_log`)\n\n```\n"
                + rows.stream().map(Object::toString).collect(java.util.stream.Collectors.joining("\n")) + "\n```\n");

        assertThat(rows).isNotEmpty().allSatisfy(r -> assertThat(r.get("channel")).isEqualTo("MCP"));
        assertThat(rows).extracting(r -> r.get("action")).contains("write:create_treatment", "write:update_spa_profile",
                "denied:create_treatment", "failed:create_treatment", "failed:update_treatment");
        assertThat(rows).filteredOn(r -> "denied:create_treatment".equals(r.get("action")))
                .allSatisfy(r -> assertThat(r.get("actor_name")).isEqualTo("Esthetician Example"));
    }

    @AfterAll
    static void writeProof() throws Exception {
        String file = System.getenv("SPACO_PROOF_FILE");
        if (file == null || file.isBlank() || TRANSCRIPT.isEmpty()) return;
        Path path = Path.of(file);
        Files.createDirectories(path.getParent());
        Files.writeString(path, "# MCP write tools: proof transcript\n\n"
                + "Generated by `McpWriteToolsDbTest`: MockMvc -> Spring Security (mocked JWT) -> `McpController` -> use cases -> "
                + "a throwaway Postgres 17 (docker) with the real Flyway migrations. Two members of `miel-spa` "
                + "(`owner@example.com` OWNER, `esthetician@example.com` ESTHETICIAN) and a second spa `otro-spa`. "
                + "No real personal data.\n\n" + String.join("\n", TRANSCRIPT));
    }

    // -- helpers ------------------------------------------------------------------------------

    private JsonNode call(String sub, String label, String tool, Map<String, Object> arguments) throws Exception {
        return rpc(sub, label, "tools/call", Map.of("name", tool, "arguments", arguments));
    }

    private int nextId = 1;

    private JsonNode rpc(String sub, String label, String method, Map<String, Object> params) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", nextId++);
        request.put("method", method);
        request.put("params", params);
        String body = json.writeValueAsString(request);
        String response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/mcp/miel-spa")
                        .with(SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.subject(sub).tokenValue("token-" + sub)))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getContentAsString();
        TRANSCRIPT.add("### " + label + "\n\nRequest (as `" + sub + "`):\n\n```json\n"
                + json.writerWithDefaultPrettyPrinter().writeValueAsString(request) + "\n```\n\nResponse:\n\n```json\n"
                + abbreviate(json.writerWithDefaultPrettyPrinter().writeValueAsString(json.readTree(response)), method) + "\n```\n");
        return json.readTree(response);
    }

    /** tools/list is long: keep the names and the first tool's full descriptor. */
    private String abbreviate(String pretty, String method) throws Exception {
        if (!method.equals("tools/list")) return pretty;
        JsonNode tree = json.readTree(pretty);
        List<Object> compact = new ArrayList<>();
        tree.at("/result/tools").forEach(t -> compact.add(Map.of("name", t.get("name").asString(),
                "annotations", json.convertValue(t.get("annotations"), Map.class))));
        return json.writerWithDefaultPrettyPrinter().writeValueAsString(Map.of("jsonrpc", "2.0", "id", tree.get("id").asInt(),
                "result", Map.of("tools (name + annotations only)", compact)));
    }

    private static void assertNotFound(JsonNode res, String code) {
        assertThat(res.at("/result/isError").asBoolean()).isTrue();
        assertThat(res.at("/result/content/0/text").asString()).startsWith(code);
    }
}
