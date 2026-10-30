package com.lsoftware.spaco.entity.member.gateway;

import com.lsoftware.spaco.entity.member.model.TenantMember;

import java.util.List;
import java.util.Optional;

public interface MemberGateway {
    TenantMember save(TenantMember member);
    Optional<TenantMember> findById(Long id);
    Optional<TenantMember> findBySub(String authcoSub);
    Optional<TenantMember> findUnlinkedByEmail(String email);
    boolean existsByEmail(String email);
    List<TenantMember> findAll();
    void deleteById(Long id);
}
