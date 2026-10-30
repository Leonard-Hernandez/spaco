package com.lsoftware.spaco.infrastructure.tenant.gateway;

import com.lsoftware.spaco.entity.tenant.gateway.TenantGateway;
import com.lsoftware.spaco.entity.tenant.model.Tenant;
import com.lsoftware.spaco.infrastructure.config.db.mapper.TenantMapper;
import com.lsoftware.spaco.infrastructure.config.db.repository.JpaTenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantJpaGateway implements TenantGateway {

    private final JpaTenantRepository repo;
    private final TenantMapper mapper;

    @Override
    public Optional<Tenant> findBySlug(String slug) {
        return repo.findBySlug(slug).map(mapper::toDomain);
    }

    @Override
    public Optional<Tenant> findById(Long id) {
        return repo.findById(id).map(mapper::toDomain);
    }
}
