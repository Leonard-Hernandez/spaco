package com.lsoftware.spaco.entity.clinical.model;

/** Who touches clinical data: a staff member (panel, MCP) or the patient herself (portal). */
public record Actor(String sub, String name, AccessChannel channel) {}
