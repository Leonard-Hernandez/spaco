package com.lsoftware.spaco.usecase.patient;

import com.lsoftware.spaco.entity.identity.gateway.IdentityGateway;
import com.lsoftware.spaco.entity.identity.model.UserIdentity;
import com.lsoftware.spaco.entity.patient.exception.PatientNotLinkedException;
import com.lsoftware.spaco.entity.patient.gateway.PatientGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.lsoftware.spaco.usecase.Fixtures.patient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResolvePatientUseCaseTest {

    private final PatientGateway patients = mock(PatientGateway.class);
    private final IdentityGateway identity = mock(IdentityGateway.class);
    private final ResolvePatientUseCase useCase = new ResolvePatientUseCase(patients, identity);

    @BeforeEach
    void setUp() {
        when(patients.findBySub("sub-1")).thenReturn(Optional.empty());
        when(patients.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void theFirstVisitLinksThePatientWithTheSameVerifiedEmail() {
        when(identity.userInfo("token")).thenReturn(new UserIdentity("sub-1", "Laura@Example.com", "Laura", true));
        when(patients.findUnlinkedByEmail("laura@example.com")).thenReturn(List.of(patient(3L)));

        assertThat(useCase.execute("sub-1", "token").authcoSub()).isEqualTo("sub-1");
    }

    @Test
    void anUnverifiedEmailLinksNothing() {
        when(identity.userInfo("token")).thenReturn(new UserIdentity("sub-1", "laura@example.com", "Laura", false));

        assertThatThrownBy(() -> useCase.execute("sub-1", "token")).isInstanceOf(PatientNotLinkedException.class);
        verify(patients, never()).save(any());
    }

    @Test
    void twoPatientsWithThatEmailAreAmbiguous() {
        when(identity.userInfo("token")).thenReturn(new UserIdentity("sub-1", "laura@example.com", "Laura", true));
        when(patients.findUnlinkedByEmail("laura@example.com")).thenReturn(List.of(patient(3L), patient(4L)));

        assertThatThrownBy(() -> useCase.execute("sub-1", "token")).isInstanceOf(PatientNotLinkedException.class);
    }

    @Test
    void anAlreadyLinkedAccountDoesNotCallAuthco() {
        when(patients.findBySub("sub-1")).thenReturn(Optional.of(patient(3L).linkedTo("sub-1")));

        assertThat(useCase.execute("sub-1", "token").id()).isEqualTo(3L);
        verifyNoInteractions(identity);
    }
}
