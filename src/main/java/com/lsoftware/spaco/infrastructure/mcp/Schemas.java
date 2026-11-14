package com.lsoftware.spaco.infrastructure.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small builder for the JSON Schemas of the tool inputs. */
final class Schemas {

    static final String TIME_PATTERN = "^([01]\\d|2[0-3]):[0-5]\\d$";

    private final Map<String, Object> properties = new LinkedHashMap<>();
    private final List<String> required = new ArrayList<>();

    static Schemas object() {
        return new Schemas();
    }

    Schemas string(String name, String description, boolean mandatory) {
        return prop(name, Map.of("type", "string", "description", description), mandatory);
    }

    /** Text with a length limit; nullable lets the caller clear it with an explicit null. */
    Schemas string(String name, String description, boolean mandatory, int maxLength, boolean nullable) {
        Map<String, Object> def = new LinkedHashMap<>();
        def.put("type", nullable ? List.of("string", "null") : "string");
        if (mandatory) def.put("minLength", 1);
        def.put("maxLength", maxLength);
        def.put("description", description);
        return prop(name, def, mandatory);
    }

    Schemas pattern(String name, String description, String regex, boolean mandatory, boolean nullable) {
        Map<String, Object> def = new LinkedHashMap<>();
        def.put("type", nullable ? List.of("string", "null") : "string");
        def.put("pattern", regex);
        def.put("description", description);
        return prop(name, def, mandatory);
    }

    Schemas time(String name, String description, boolean mandatory) {
        return pattern(name, description + " (24h HH:mm)", TIME_PATTERN, mandatory, false);
    }

    Schemas integer(String name, String description, boolean mandatory) {
        return prop(name, Map.of("type", "integer", "description", description), mandatory);
    }

    Schemas integer(String name, String description, boolean mandatory, long min, Long max, boolean nullable) {
        Map<String, Object> def = new LinkedHashMap<>();
        def.put("type", nullable ? List.of("integer", "null") : "integer");
        def.put("minimum", min);
        if (max != null) def.put("maximum", max);
        def.put("description", description);
        return prop(name, def, mandatory);
    }

    Schemas bool(String name, String description) {
        return prop(name, Map.of("type", "boolean", "description", description), false);
    }

    Schemas stringList(String name, String description, int maxItems, int maxLength) {
        return prop(name, Map.of("type", "array", "maxItems", maxItems,
                "items", Map.of("type", "string", "minLength", 1, "maxLength", maxLength),
                "description", description), false);
    }

    Schemas date(String name, String description, boolean mandatory) {
        return prop(name, Map.of("type", "string", "format", "date", "description", description), mandatory);
    }

    Schemas enumeration(String name, String description, List<String> values, boolean mandatory) {
        return prop(name, Map.of("type", "string", "enum", values, "description", description), mandatory);
    }

    Map<String, Object> build() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private Schemas prop(String name, Map<String, Object> definition, boolean mandatory) {
        properties.put(name, definition);
        if (mandatory) required.add(name);
        return this;
    }
}
