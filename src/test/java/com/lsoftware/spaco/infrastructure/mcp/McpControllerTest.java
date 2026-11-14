package com.lsoftware.spaco.infrastructure.mcp;

import com.lsoftware.spaco.entity.clinical.model.AccessChannel;
import com.lsoftware.spaco.entity.exception.DomainException;
import com.lsoftware.spaco.entity.member.model.MemberRole;
import com.lsoftware.spaco.entity.member.model.TenantMember;
import com.lsoftware.spaco.entity.tenant.gateway.TenantGateway;
import com.lsoftware.spaco.usecase.clinical.ClinicalAudit;
import com.lsoftware.spaco.usecase.member.ResolveMemberUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.lsoftware.spaco.usecase.Fixtures.SPA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpControllerTest {

    private final TenantGateway tenants = mock(TenantGateway.class);
    private final ResolveMemberUseCase members = mock(ResolveMemberUseCase.class);
    private final SpaTools spaTools = mock(SpaTools.class);
    private final SpaAdminTools adminTools = mock(SpaAdminTools.class);
    private final ClinicalAudit audit = mock(ClinicalAudit.class);
    private final McpController controller = new McpController(tenants, members, spaTools, adminTools, audit,
            JsonMapper.builder().build());
    private final Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("sub-ana")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    private final AtomicReference<String> seenChannel = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        when(tenants.findBySlug("miel-spa")).thenReturn(Optional.of(SPA));
        member(MemberRole.ESTHETICIAN);
        when(adminTools.tools()).thenReturn(List.of());
        when(spaTools.tools()).thenReturn(List.of(
                new McpTool("echo", "Echo", "Returns its input", Schemas.object().string("text", "Text", true).build(),
                        (args, actor) -> {
                            seenChannel.set(actor.channel().name());
                            return Map.of("echo", args.requiredString("text"));
                        }),
                new McpTool("fails", "Fails", "Always fails", Schemas.object().build(),
                        (args, actor) -> { throw new DomainException("PATIENT_NOT_FOUND", "Patient not found"); })));
    }

    @Test
    @SuppressWarnings("unchecked")
    void initializeNegotiatesTheProtocolVersion() {
        var response = (Map<String, Object>) controller.handle("miel-spa",
                rpc(1, "initialize", Map.of("protocolVersion", "2025-03-26")), jwt).getBody();

        var result = (Map<String, Object>) response.get("result");
        assertThat(result.get("protocolVersion")).isEqualTo("2025-03-26");
        assertThat(result).containsKey("instructions");
    }

    @Test
    void notificationsAreAcceptedWithoutBody() {
        var response = controller.handle("miel-spa", Map.of("jsonrpc", "2.0", "method", "notifications/initialized"), jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void toolCallsRunAsTheMemberThroughTheMcpChannel() {
        var response = (Map<String, Object>) controller.handle("miel-spa",
                rpc(2, "tools/call", Map.of("name", "echo", "arguments", Map.of("text", "hola"))), jwt).getBody();

        var result = (Map<String, Object>) response.get("result");
        assertThat(result.get("isError")).isEqualTo(false);
        assertThat(((List<Map<String, Object>>) result.get("content")).getFirst().get("text")).isEqualTo("{\"echo\":\"hola\"}");
        assertThat(seenChannel.get()).isEqualTo(AccessChannel.MCP.name());
    }

    @Test
    @SuppressWarnings("unchecked")
    void domainErrorsComeBackAsToolErrorsTheModelCanRead() {
        var response = (Map<String, Object>) controller.handle("miel-spa",
                rpc(3, "tools/call", Map.of("name", "fails")), jwt).getBody();

        var result = (Map<String, Object>) response.get("result");
        assertThat(result.get("isError")).isEqualTo(true);
        assertThat(result.toString()).contains("PATIENT_NOT_FOUND");
    }

    @Test
    void theFrontDeskCannotUseIt() {
        member(MemberRole.RECEPTION);

        assertThat(controller.handle("miel-spa", rpc(4, "tools/list", Map.of()), jwt).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unknownSpasAreNotFound() {
        when(tenants.findBySlug("otro")).thenReturn(Optional.empty());

        assertThat(controller.handle("otro", rpc(5, "tools/list", Map.of()), jwt).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @SuppressWarnings("unchecked")
    void writeToolsAreListedWithAnnotationsForTheOwnerOnly() {
        when(adminTools.tools()).thenReturn(List.of(
                McpTool.create("make", "Make", "Creates a thing", Schemas.object().string("name", "Name", true, 10, false).build(),
                        (args, actor) -> Map.of("ok", true)),
                McpTool.update("change", "Change", "Changes a thing", Schemas.object().build(), (args, actor) -> Map.of())));

        assertThat(toolNames(controller.handle("miel-spa", rpc(6, "tools/list", Map.of()), jwt).getBody()))
                .containsExactly("echo", "fails");

        member(MemberRole.OWNER);
        var tools = tools(controller.handle("miel-spa", rpc(7, "tools/list", Map.of()), jwt).getBody());
        assertThat(tools).extracting(t -> t.get("name")).containsExactly("echo", "fails", "make", "change");
        assertThat((Map<String, Object>) tools.get(0).get("annotations")).containsEntry("readOnlyHint", true);
        assertThat((Map<String, Object>) tools.get(2).get("annotations"))
                .containsEntry("readOnlyHint", false).containsEntry("destructiveHint", false).containsEntry("idempotentHint", false);
        assertThat((Map<String, Object>) tools.get(3).get("annotations"))
                .containsEntry("readOnlyHint", false).containsEntry("destructiveHint", true).containsEntry("idempotentHint", true);
    }

    @Test
    void anEstheticianCallingAWriteToolGetsAClearErrorAndNothingRuns() {
        AtomicReference<Boolean> ran = new AtomicReference<>(false);
        when(adminTools.tools()).thenReturn(List.of(McpTool.create("make", "Make", "Creates a thing",
                Schemas.object().build(), (args, actor) -> { ran.set(true); return Map.of(); })));

        var result = callResult(controller.handle("miel-spa", rpc(8, "tools/call", Map.of("name", "make")), jwt).getBody());

        assertThat(result.get("isError")).isEqualTo(true);
        assertThat(result.toString()).contains("FORBIDDEN").contains("only available to its owner");
        assertThat(ran.get()).isFalse();
        verify(audit).record(eq(null), any(), eq("denied:make"));
    }

    @Test
    void ownerWritesAreValidatedAgainstTheSchemaAndAudited() {
        member(MemberRole.OWNER);
        AtomicReference<Boolean> ran = new AtomicReference<>(false);
        when(adminTools.tools()).thenReturn(List.of(McpTool.create("make", "Make", "Creates a thing",
                Schemas.object().string("name", "Name", true, 5, false).integer("size", "Size", false, 1, 9L, false).build(),
                (args, actor) -> { ran.set(true); return Map.of("name", args.requiredString("name")); })));

        for (Map<String, Object> bad : List.of(Map.<String, Object>of(), Map.<String, Object>of("name", "toolongname"),
                Map.<String, Object>of("name", "ok", "size", 10), Map.<String, Object>of("name", "ok", "size", "x"),
                Map.<String, Object>of("name", "ok", "extra", 1))) {
            var result = callResult(controller.handle("miel-spa",
                    rpc(9, "tools/call", Map.of("name", "make", "arguments", bad)), jwt).getBody());
            assertThat(result.get("isError")).as(bad.toString()).isEqualTo(true);
            assertThat(result.toString()).contains("INVALID_ARGUMENT");
        }
        assertThat(ran.get()).isFalse();
        verify(audit, times(5)).record(eq(null), any(), eq("failed:make"));

        var ok = callResult(controller.handle("miel-spa",
                rpc(10, "tools/call", Map.of("name", "make", "arguments", Map.of("name", "ok", "size", 3))), jwt).getBody());
        assertThat(ok.get("isError")).isEqualTo(false);
        verify(audit).record(eq(null), any(), eq("write:make"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void anUnexpectedFailureInAWriteToolIsStillAuditedAndReturnsAGenericError() {
        member(MemberRole.OWNER);
        when(adminTools.tools()).thenReturn(List.of(McpTool.create("make", "Make", "Creates a thing",
                Schemas.object().build(), (args, actor) -> { throw new IllegalStateException("db down"); })));

        var body = (Map<String, Object>) controller.handle("miel-spa",
                rpc(11, "tools/call", Map.of("name", "make")), jwt).getBody();

        assertThat(body.get("error").toString()).contains("Internal error").doesNotContain("db down");
        verify(audit).record(eq(null), any(), eq("failed:make"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> tools(Object body) {
        return (List<Map<String, Object>>) ((Map<String, Object>) ((Map<String, Object>) body).get("result")).get("tools");
    }

    private static List<Object> toolNames(Object body) {
        return tools(body).stream().map(t -> t.get("name")).toList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> callResult(Object body) {
        return (Map<String, Object>) ((Map<String, Object>) body).get("result");
    }

    private void member(MemberRole role) {
        when(members.execute("sub-ana", "token")).thenReturn(Optional.of(
                new TenantMember(1L, "ana@example.com", "sub-ana", "Ana", role)));
    }

    private static Map<String, Object> rpc(int id, String method, Map<String, Object> params) {
        return Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", params);
    }
}
