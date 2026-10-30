package com.lsoftware.spaco.infrastructure.media.controller;

import org.springframework.http.MediaType;

public final class MediaTypes {

    private MediaTypes() {}

    public static MediaType of(String file) {
        if (file.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (file.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }
}
