package com.lsoftware.spaco.entity.identity.model;

public record UserIdentity(
        String sub,
        String email,
        String name,
        boolean emailVerified
) {}
