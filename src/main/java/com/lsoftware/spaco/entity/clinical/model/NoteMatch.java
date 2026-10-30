package com.lsoftware.spaco.entity.clinical.model;

import java.time.OffsetDateTime;

/** A session note found by full-text search, with a highlighted excerpt. */
public record NoteMatch(Long sessionNoteId, Long patientId, String patientName, OffsetDateTime performedAt,
                        String treatmentName, String excerpt) {}
