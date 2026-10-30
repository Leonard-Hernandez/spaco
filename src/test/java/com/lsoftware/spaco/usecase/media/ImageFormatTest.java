package com.lsoftware.spaco.usecase.media;

import com.lsoftware.spaco.entity.media.exception.InvalidImageException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageFormatTest {

    @Test
    void jpegLosesItsExifSegmentButKeepsTheRest() {
        byte[] jpeg = jpeg(true);

        byte[] clean = ImageFormat.withoutMetadata(jpeg, ImageFormat.extensionOf(jpeg));

        assertThat(clean).isEqualTo(jpeg(false));
    }

    @Test
    void anythingButJpgPngOrWebpIsRejected() {
        assertThatThrownBy(() -> ImageFormat.extensionOf("GIF89a-not-a-photo".getBytes()))
                .isInstanceOf(InvalidImageException.class);
    }

    /** SOI, [APP1 with "Exif GPS"], APP0, SOS + data, EOI */
    private static byte[] jpeg(boolean withExif) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{(byte) 0xFF, (byte) 0xD8});
        if (withExif) out.writeBytes(new byte[]{(byte) 0xFF, (byte) 0xE1, 0, 10, 'E', 'x', 'i', 'f', 'G', 'P', 'S', 0});
        out.writeBytes(new byte[]{(byte) 0xFF, (byte) 0xE0, 0, 6, 'J', 'F', 'I', 'F'});
        out.writeBytes(new byte[]{(byte) 0xFF, (byte) 0xDA, 0, 4, 1, 2, 9, 9, 9, (byte) 0xFF, (byte) 0xD9});
        return out.toByteArray();
    }
}
