package com.lsoftware.spaco.infrastructure.media.controller;

import com.lsoftware.spaco.entity.media.gateway.PublicImageGateway;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Public photos (treatments, professionals, published testimonials). Outside /api (no X-Tenant
 * header: an img tag cannot send one), so the spa is in the path. File names are random and
 * never reused: cache them for good. Clinical photos live in another folder and never come here.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Media (public)", description = "Treatment, professional and testimonial photos")
public class MediaController {

    private final PublicImageGateway images;

    @GetMapping("/media/{tenant}/{file}")
    @Operation(summary = "Public photo")
    public ResponseEntity<Resource> image(@PathVariable String tenant, @PathVariable String file) {
        return images.find(tenant, file)
                .<ResponseEntity<Resource>>map(path -> ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                        .contentType(MediaTypes.of(file))
                        .body(new FileSystemResource(path)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
