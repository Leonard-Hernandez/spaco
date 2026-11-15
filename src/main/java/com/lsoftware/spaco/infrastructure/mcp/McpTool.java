package com.lsoftware.spaco.infrastructure.mcp;

import com.lsoftware.spaco.entity.clinical.model.Actor;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * One tool the assistant can call. inputSchema is a JSON Schema object; handler receives the
 * call arguments and who is asking (for the access log) and returns any value, sent back as JSON.
 * A write tool changes the spa and is only offered to, and callable by, its owner; destructive and
 * idempotent are the MCP annotation hints (destructive: overwrites existing data).
 */
public record McpTool(String name, String title, String description, Map<String, Object> inputSchema,
                      BiFunction<McpArgs, Actor, Object> handler,
                      boolean write, boolean destructive, boolean idempotent) {

    /** A read-only tool. */
    public McpTool(String name, String title, String description, Map<String, Object> inputSchema,
                   BiFunction<McpArgs, Actor, Object> handler) {
        this(name, title, description, inputSchema, handler, false, false, false);
    }

    /** Creates a record: not idempotent, nothing is overwritten. */
    public static McpTool create(String name, String title, String description, Map<String, Object> inputSchema,
                                 BiFunction<McpArgs, Actor, Object> handler) {
        return new McpTool(name, title, description, inputSchema, handler, true, false, false);
    }

    /** Changes a record: it overwrites fields, and repeating the same call has no further effect. */
    public static McpTool update(String name, String title, String description, Map<String, Object> inputSchema,
                                 BiFunction<McpArgs, Actor, Object> handler) {
        return new McpTool(name, title, description, inputSchema, handler, true, true, true);
    }

    public Map<String, Object> descriptor() {
        Map<String, Object> annotations = write
                ? Map.of("readOnlyHint", false, "destructiveHint", destructive, "idempotentHint", idempotent,
                        "openWorldHint", false)
                : Map.of("readOnlyHint", true, "openWorldHint", false);
        return Map.of(
                "name", name,
                "title", title,
                "description", description,
                "inputSchema", inputSchema,
                "annotations", annotations);
    }
}
