package com.lsoftware.spaco.infrastructure.clinical.controller;

import com.lsoftware.spaco.infrastructure.media.controller.MediaTypes;
import com.lsoftware.spaco.usecase.clinical.dto.PhotoFile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

import java.time.Duration;

/** Clinical photos: only the browser of whoever asked may keep them, and briefly. */
public final class PhotoResponses {

    private PhotoResponses() {}

    public static ResponseEntity<Resource> of(PhotoFile file) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePrivate())
                .contentType(MediaTypes.of(file.photo().fileName()))
                .body(new FileSystemResource(file.path()));
    }
}
