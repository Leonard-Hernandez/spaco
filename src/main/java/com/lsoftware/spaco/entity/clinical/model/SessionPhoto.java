package com.lsoftware.spaco.entity.clinical.model;

import java.time.OffsetDateTime;

/** fileName lives in the private clinical store. */
public record SessionPhoto(Long id, Long sessionNoteId, PhotoKind kind, String fileName, String caption,
                           OffsetDateTime createdAt) {}
