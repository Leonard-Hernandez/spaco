package com.lsoftware.spaco.entity.media.gateway;

import java.nio.file.Path;
import java.util.Optional;

/** Image files stored per spa. */
public interface FileStore {
    /** Stores the image and returns its file name. */
    String store(String tenantSlug, byte[] content, String extension);
    void delete(String tenantSlug, String fileName);
    Optional<Path> find(String tenantSlug, String fileName);
}
