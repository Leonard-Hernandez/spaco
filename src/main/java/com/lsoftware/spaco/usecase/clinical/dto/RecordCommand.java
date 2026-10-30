package com.lsoftware.spaco.usecase.clinical.dto;

public record RecordCommand(
        String allergies,
        String medications,
        String conditions,
        String pregnancy,
        String skinType,
        Integer phototype,
        String surgeries,
        String habits,
        String observations
) {}
