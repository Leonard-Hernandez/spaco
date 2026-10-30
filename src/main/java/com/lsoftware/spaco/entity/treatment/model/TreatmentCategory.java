package com.lsoftware.spaco.entity.treatment.model;

/** ink: label color on the site (honey, lavender, sage, navy, clay). */
public record TreatmentCategory(Long id, String name, String slug, String ink, int position, boolean active) {}
