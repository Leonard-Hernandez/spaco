package com.lsoftware.spaco.infrastructure.member.dto;

import com.lsoftware.spaco.entity.member.model.MemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InviteRequest(@NotBlank @Email @Size(max = 150) String email, @NotNull MemberRole role) {}
