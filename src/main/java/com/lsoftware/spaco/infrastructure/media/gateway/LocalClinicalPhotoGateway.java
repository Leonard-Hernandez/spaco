package com.lsoftware.spaco.infrastructure.media.gateway;

import com.lsoftware.spaco.entity.media.gateway.ClinicalPhotoGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/** A sibling folder of the public one, which MediaController cannot reach. */
@Component
public class LocalClinicalPhotoGateway extends LocalFileStore implements ClinicalPhotoGateway {

    public LocalClinicalPhotoGateway(@Value("${spaco.uploads.dir}") String dir) {
        super(Path.of(dir, "private"));
    }
}
