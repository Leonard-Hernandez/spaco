package com.lsoftware.spaco.usecase.patient;

import com.lsoftware.spaco.entity.identity.gateway.IdentityGateway;
import com.lsoftware.spaco.entity.identity.model.UserIdentity;
import com.lsoftware.spaco.entity.patient.exception.PatientNotLinkedException;
import com.lsoftware.spaco.entity.patient.gateway.PatientGateway;
import com.lsoftware.spaco.entity.patient.model.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * The patient behind a Google account, for her history page. The first visit links the account
 * to the patient registered (at booking) with the same verified email; only an unambiguous
 * match is linked, otherwise the spa has to fix the email first.
 */
@Component
@RequiredArgsConstructor
public class ResolvePatientUseCase {

    private final PatientGateway patients;
    private final IdentityGateway identity;

    @Transactional
    public Patient execute(String sub, String accessToken) {
        var linked = patients.findBySub(sub);
        if (linked.isPresent()) return linked.get();

        UserIdentity user = identity.userInfo(accessToken);
        if (!user.emailVerified() || user.email() == null) throw new PatientNotLinkedException();
        List<Patient> matches = patients.findUnlinkedByEmail(user.email().toLowerCase(Locale.ROOT));
        if (matches.size() != 1) throw new PatientNotLinkedException();
        return patients.save(matches.getFirst().linkedTo(sub));
    }
}
