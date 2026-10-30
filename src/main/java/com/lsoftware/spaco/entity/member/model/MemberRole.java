package com.lsoftware.spaco.entity.member.model;

public enum MemberRole {
    /** Everything: catalogue, schedule, team, consents, plus every staff screen. */
    OWNER,
    /** Treats patients: agenda, clinical records, session notes and photos, testimonials, MCP. */
    ESTHETICIAN,
    /** Front desk: booking requests, agenda and patient contact data. No clinical data. */
    RECEPTION;

    /** Who may read and write clinical records (panel and MCP). */
    public boolean clinical() {
        return this == OWNER || this == ESTHETICIAN;
    }
}
