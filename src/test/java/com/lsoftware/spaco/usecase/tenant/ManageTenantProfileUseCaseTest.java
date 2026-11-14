package com.lsoftware.spaco.usecase.tenant;

import com.lsoftware.spaco.entity.exception.DomainException;
import com.lsoftware.spaco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.spaco.entity.tenant.gateway.TenantGateway;
import com.lsoftware.spaco.entity.tenant.model.Tenant;
import com.lsoftware.spaco.usecase.tenant.dto.TenantProfileCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static com.lsoftware.spaco.usecase.Fixtures.BOGOTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ManageTenantProfileUseCaseTest {

    private final CurrentTenantGateway current = mock(CurrentTenantGateway.class);
    private final TenantGateway tenants = mock(TenantGateway.class);
    private final ManageTenantProfileUseCase useCase = new ManageTenantProfileUseCase(current, tenants);

    @BeforeEach
    void setUp() {
        Tenant spa = new Tenant(1L, "miel-spa", "Miel Spa", BOGOTA, null, "[DIRECCIÓN]", 30, 120, Map.of(
                "wordmark", "miel", "logoMark", "/assets/logo.webp", "heroTreatment", "ritual-miel",
                "theme", Map.of("amber", "#d08a1c"), "hours", "[HORARIO]", "whatsapp", "[NÚMERO]",
                "email", "[CORREO]", "instagram", "[@INSTAGRAM]"));
        when(current.current()).thenReturn(spa);
        when(tenants.updateProfile(eq(1L), any(), any(), any())).thenAnswer(inv ->
                new Tenant(1L, "miel-spa", "Miel Spa", BOGOTA, inv.getArgument(1), inv.getArgument(2), 30, 120, inv.getArgument(3)));
    }

    @Test
    void updatingKeepsTheKeysItDoesNotManage() {
        useCase.update(new TenantProfileCommand("300 111 2233", "hola@miel.co", "Calle 1 # 2-3", "Cali", "Dulzura", null,
                "miel_spa", null));

        Map<String, Object> branding = savedBranding();
        assertThat(branding).containsEntry("wordmark", "miel").containsEntry("logoMark", "/assets/logo.webp")
                .containsEntry("heroTreatment", "ritual-miel").containsKey("theme")
                .containsEntry("email", "hola@miel.co").containsEntry("city", "Cali")
                .containsEntry("instagram", "@miel_spa");
    }

    @Test
    void phoneIsStoredOnceInBothPlacesSoTheyCannotDisagree() {
        Tenant saved = useCase.update(new TenantProfileCommand("300 111 2233", null, null, null, null, null, null, null));

        assertThat(saved.contactPhone()).isEqualTo("+573001112233");
        assertThat(saved.branding()).containsEntry("whatsapp", "+573001112233");
    }

    @Test
    void blankFieldsClearThePlaceholdersAndTheStaleHoursText() {
        Tenant saved = useCase.update(new TenantProfileCommand(" ", "", null, null, null, null, null, null));

        assertThat(saved.contactPhone()).isNull();
        assertThat(saved.address()).isNull();
        assertThat(saved.branding()).doesNotContainKeys("whatsapp", "email", "instagram", "hours", "address");
    }

    @Test
    void anInvalidPhoneIsRejectedBeforeSaving() {
        assertThatThrownBy(() -> useCase.update(new TenantProfileCommand("123", null, null, null, null, null, null, null)))
                .isInstanceOf(DomainException.class);
        verify(tenants, never()).updateProfile(any(), any(), any(), any());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> savedBranding() {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(tenants).updateProfile(eq(1L), any(), any(), captor.capture());
        return captor.getValue();
    }
}
