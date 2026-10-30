package com.lsoftware.spaco.usecase.clinical.dto;

import com.lsoftware.spaco.entity.clinical.model.SessionPhoto;

import java.nio.file.Path;

public record PhotoFile(SessionPhoto photo, Path path) {}
