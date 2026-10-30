package com.lsoftware.spaco.entity.consent.model;

/** The text a patient accepts. Changing it creates a new version. */
public record ConsentTemplate(Long id, ConsentType type, int version, String title, String body, boolean active) {}
