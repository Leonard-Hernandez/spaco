package com.lsoftware.spaco.usecase.media;

import com.lsoftware.spaco.entity.media.exception.InvalidImageException;

import java.io.ByteArrayOutputStream;

/** Detects the photo format from its first bytes; the declared content type is not trusted. */
public final class ImageFormat {

    public static final int MAX_BYTES = 8 * 1024 * 1024;

    private ImageFormat() {}

    public static String extensionOf(byte[] b) {
        if (b == null || b.length < 12 || b.length > MAX_BYTES) throw new InvalidImageException();
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "webp";
        throw new InvalidImageException();
    }

    /**
     * Drops the APP1 segments (EXIF/XMP: camera, date and GPS position) of a JPEG taken with a
     * phone, so a patient photo never carries where it was taken. Other formats pass unchanged.
     */
    public static byte[] withoutMetadata(byte[] b, String extension) {
        if (!"jpg".equals(extension)) return b;
        ByteArrayOutputStream out = new ByteArrayOutputStream(b.length);
        out.write(b, 0, 2);
        int i = 2;
        while (i + 4 <= b.length && (b[i] & 0xFF) == 0xFF) {
            int marker = b[i + 1] & 0xFF;
            // Start of scan: the compressed image follows to the end
            if (marker == 0xDA) break;
            int length = ((b[i + 2] & 0xFF) << 8) | (b[i + 3] & 0xFF);
            if (length < 2 || i + 2 + length > b.length) return b;
            if (marker != 0xE1) out.write(b, i, 2 + length);
            i += 2 + length;
        }
        out.write(b, i, b.length - i);
        return out.toByteArray();
    }
}
