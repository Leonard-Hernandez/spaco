package com.lsoftware.spaco.entity.professional.model;

/** Who performs treatments. memberId links the panel account, when there is one. */
public record Professional(
        Long id,
        String displayName,
        String title,
        String bio,
        String photoPath,
        Long memberId,
        boolean active
) {
    public Professional withPhoto(String path) {
        return new Professional(id, displayName, title, bio, path, memberId, active);
    }
}
