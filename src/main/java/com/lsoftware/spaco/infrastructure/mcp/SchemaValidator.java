package com.lsoftware.spaco.infrastructure.mcp;

import com.lsoftware.spaco.entity.exception.DomainException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Checks tool arguments against the subset of JSON Schema that {@link Schemas} produces (type,
 * enum, minimum/maximum, minLength/maxLength, pattern, items/maxItems, required and
 * additionalProperties), so write tools reject bad input before touching any data.
 */
final class SchemaValidator {

    private SchemaValidator() {}

    @SuppressWarnings("unchecked")
    static void validate(Map<String, Object> schema, Map<String, Object> arguments) {
        List<String> errors = new ArrayList<>();
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        List<String> required = (List<String>) schema.getOrDefault("required", List.of());
        for (String name : required) {
            if (arguments.get(name) == null) errors.add(name + " is required");
        }
        for (Map.Entry<String, Object> arg : arguments.entrySet()) {
            Object definition = properties.get(arg.getKey());
            if (definition == null) {
                errors.add("unknown argument: " + arg.getKey());
            } else if (arg.getValue() != null || required.contains(arg.getKey())) {
                check(arg.getKey(), (Map<String, Object>) definition, arg.getValue(), errors);
            } else {
                // Explicit null is only valid where the schema says the field is nullable
                if (!types((Map<String, Object>) definition).contains("null")) {
                    errors.add(arg.getKey() + " cannot be null");
                }
            }
        }
        if (!errors.isEmpty()) throw new DomainException("INVALID_ARGUMENT", String.join("; ", errors));
    }

    @SuppressWarnings("unchecked")
    private static void check(String path, Map<String, Object> def, Object value, List<String> errors) {
        if (value == null) {
            if (!types(def).contains("null")) errors.add(path + " cannot be null");
            return;
        }
        List<String> types = types(def);
        if (def.containsKey("enum") && !((List<Object>) def.get("enum")).contains(value)) {
            errors.add(path + " must be one of " + def.get("enum"));
            return;
        }
        if (value instanceof String s && types.contains("string")) {
            if (def.get("minLength") instanceof Integer min && s.strip().length() < min) {
                errors.add(path + " cannot be blank");
            }
            if (def.get("maxLength") instanceof Integer max && s.length() > max) {
                errors.add(path + " must have at most " + max + " characters");
            }
            if (def.get("pattern") instanceof String p && !Pattern.compile(p).matcher(s).find()) {
                errors.add(path + " has an invalid format");
            }
        } else if (isInteger(value) && types.contains("integer")) {
            long n = ((Number) value).longValue();
            if (def.get("minimum") instanceof Number min && n < min.longValue()) errors.add(path + " must be at least " + min);
            if (def.get("maximum") instanceof Number max && n > max.longValue()) errors.add(path + " must be at most " + max);
        } else if (value instanceof Boolean && types.contains("boolean")) {
            // ok
        } else if (value instanceof List<?> list && types.contains("array")) {
            if (def.get("maxItems") instanceof Integer max && list.size() > max) {
                errors.add(path + " must have at most " + max + " items");
            }
            Map<String, Object> items = (Map<String, Object>) def.get("items");
            for (int i = 0; items != null && i < list.size(); i++) check(path + "[" + i + "]", items, list.get(i), errors);
        } else {
            errors.add(path + " must be of type " + String.join(" or ", types));
        }
    }

    private static boolean isInteger(Object v) {
        return v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte;
    }

    @SuppressWarnings("unchecked")
    private static List<String> types(Map<String, Object> def) {
        Object t = def.get("type");
        return t instanceof List<?> l ? (List<String>) l : List.of(String.valueOf(t));
    }
}
