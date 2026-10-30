package com.lsoftware.spaco.infrastructure.appointment;

import com.lsoftware.spaco.entity.tenant.model.Tenant;
import com.lsoftware.spaco.infrastructure.config.db.mapper.TenantMapper;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaTenantRepository;
import com.lsoftware.spaco.infrastructure.config.tenant.TenantContext;
import com.lsoftware.spaco.usecase.appointment.ManageAppointmentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/** Closes requests whose start time passed without an answer, spa by spa. */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentExpiryJob {

    private final JpaTenantRepository tenants;
    private final TenantMapper tenantMapper;
    private final ManageAppointmentUseCase appointments;

    @Scheduled(fixedDelayString = "${spaco.appointments.expiry-check:PT10M}", initialDelayString = "PT1M")
    public void run() {
        List<Tenant> all = TenantContext.asSystem(() -> tenants.findAll().stream().map(tenantMapper::toDomain).toList());
        for (Tenant tenant : all) {
            int expired = TenantContext.as(tenant, appointments::expireUnanswered);
            if (expired > 0) log.info("Closed {} unanswered requests of {}", expired, tenant.slug());
        }
    }
}
