package com.lsoftware.spaco.infrastructure.media.gateway;

import com.lsoftware.spaco.entity.exception.DomainException;
import com.lsoftware.spaco.entity.media.gateway.FileStore;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Files on a local volume: {root}/{tenant slug}/{uuid}.{ext}. */
@Slf4j
abstract class LocalFileStore implements FileStore {

    // Only names this class generates: no path traversal through slug or file name
    private static final Pattern SLUG = Pattern.compile("[a-z0-9][a-z0-9-]{0,59}");
    private static final Pattern FILE = Pattern.compile("[0-9a-f-]{36}\\.(jpg|png|webp)");

    private final Path root;

    LocalFileStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public String store(String tenantSlug, byte[] content, String extension) {
        String file = UUID.randomUUID() + "." + extension;
        try {
            Path folder = Files.createDirectories(folder(tenantSlug));
            Files.write(folder.resolve(file), content);
            return file;
        } catch (IOException e) {
            throw new DomainException("STORAGE_UNAVAILABLE", "Could not save the photo");
        }
    }

    @Override
    public void delete(String tenantSlug, String fileName) {
        find(tenantSlug, fileName).ifPresent(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Could not delete {}", path, e);
            }
        });
    }

    @Override
    public Optional<Path> find(String tenantSlug, String fileName) {
        if (!SLUG.matcher(tenantSlug).matches() || !FILE.matcher(fileName).matches()) return Optional.empty();
        Path path = folder(tenantSlug).resolve(fileName);
        return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
    }

    private Path folder(String tenantSlug) {
        if (!SLUG.matcher(tenantSlug).matches()) throw new IllegalArgumentException("Invalid tenant slug");
        return root.resolve(tenantSlug);
    }
}
