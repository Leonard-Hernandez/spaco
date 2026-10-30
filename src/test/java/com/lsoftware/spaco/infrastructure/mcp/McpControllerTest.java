package com.lsoftware.spaco.infrastructure.mcp;

import com.lsoftware.spaco.entity.clinical.model.AccessChannel;
import com.lsoftware.spaco.entity.exception.DomainException;
import com.lsoftware.spaco.entity.member.model.MemberRole;
import com.lsoftware.spaco.entity.member.model.TenantMember;
import com.lsoftware.spaco.entity.tenant.gateway.TenantGateway;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpControllerTest {

    private final TenantGateway tenants = mock(TenantGateway.class);
    private final ResolveMemberUseCase members = mock(ResolveMemberUseCase.class);
    private final SpaTools spaTools = mock(SpaTools.class);
    private final McpController controller = new McpController(tenants, members, spaTools, JsonMapper.builder().build());
    private final Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("sub-ana")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
    private final AtomicReference<String> seenChannel = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        when(tenants.findBySlug("miel-spa")).thenReturn(Optional.of(SPA));
        member(MemberRole.ESTHETICIAN);
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

    private void member(MemberRole role) {
        when(members.execute("sub-ana", "token")).thenReturn(Optional.of(
                new TenantMember(1L, "ana@example.com", "sub-ana", "Ana", role)));
    }

    private static Map<String, Object> rpc(int id, String method, Map<String, Object> params) {
        return Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", params);
    }
}
