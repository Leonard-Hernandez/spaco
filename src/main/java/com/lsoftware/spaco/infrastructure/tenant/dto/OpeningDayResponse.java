package com.lsoftware.spaco.infrastructure.tenant.dto;

import com.lsoftware.spaco.entity.availability.model.OpeningDay;

import java.util.List;

/** Weekday as ISO number (1 = Monday ... 7 = Sunday); times as "HH:mm". */
public record OpeningDayResponse(int weekday, List<Block> blocks) {

    public record Block(String start, String end) {}

    public static OpeningDayResponse from(OpeningDay d) {
        return new OpeningDayResponse(d.weekday().getValue(), d.blocks().stream()
                .map(b -> new Block(b.start().toString(), b.end().toString())).toList());
    }
}
