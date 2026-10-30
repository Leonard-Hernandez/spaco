package com.lsoftware.spaco.infrastructure.member.dto;

import com.lsoftware.spaco.entity.member.model.MemberRole;
import jakarta.validation.constraints.NotNull;

public record RoleRequest(@NotNull MemberRole role) {}
