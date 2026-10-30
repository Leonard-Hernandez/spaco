package com.lsoftware.spaco.infrastructure.media.gateway;

import com.lsoftware.spaco.entity.media.gateway.PublicImageGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class LocalPublicImageGateway extends LocalFileStore implements PublicImageGateway {

    public LocalPublicImageGateway(@Value("${spaco.uploads.dir}") String dir) {
        super(Path.of(dir, "public"));
    }
}
