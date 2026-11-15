package com.lsoftware.spaco.entity.availability.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/** When the spa is open on a weekday: the merged working blocks of all its professionals. */
public record OpeningDay(DayOfWeek weekday, List<Block> blocks) {

    public record Block(LocalTime start, LocalTime end) {}
}
