package com.lsoftware.spaco.infrastructure.mcp;

import com.lsoftware.spaco.entity.clinical.model.Actor;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * One tool the assistant can call. inputSchema is a JSON Schema object; handler receives the
 * call arguments and who is asking (for the access log) and returns any value, sent back as JSON.
 */
public record McpTool(String name, String title, String description, Map<String, Object> inputSchema,
                      BiFunction<McpArgs, Actor, Object> handler) {

    public Map<String, Object> descriptor() {
        return Map.of(
                "name", name,
                "title", title,
                "description", description,
                "inputSchema", inputSchema,
                "annotations", Map.of("readOnlyHint", true, "openWorldHint", false));
    }
}
