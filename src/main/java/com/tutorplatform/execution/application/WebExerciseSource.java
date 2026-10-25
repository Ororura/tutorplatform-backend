package com.tutorplatform.execution.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;

/** The starter manifest is teacher-owned; student input contains only the three source files. */
public final class WebExerciseSource {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> FILES = Set.of("html", "css", "javascript");

    public enum ReviewMode {
        AUTO,
        MANUAL,
        HYBRID
    }

    public static JsonNode manifest(String value) {
        JsonNode node = parse(value);
        for (String file : FILES) requireText(node, file);
        if (!node.path("editableFiles").isArray() || node.path("editableFiles").isEmpty()) {
            throw new IllegalArgumentException("editableFiles must contain source file names");
        }
        Set<String> editable = new HashSet<>();
        for (JsonNode file : node.path("editableFiles")) {
            if (!file.isTextual()
                    || !FILES.contains(file.asText())
                    || !editable.add(file.asText())) {
                throw new IllegalArgumentException(
                        "editableFiles must contain unique source file names");
            }
        }
        ReviewMode.valueOf(node.path("reviewMode").asText("AUTO"));
        return node;
    }

    public static ReviewMode reviewMode(String starter) {
        return ReviewMode.valueOf(manifest(starter).path("reviewMode").asText("AUTO"));
    }

    public static String normalize(String starter, String source) {
        JsonNode config = manifest(starter);
        JsonNode input = parse(source);
        Set<String> names = new HashSet<>();
        input.fieldNames().forEachRemaining(names::add);
        if (!names.equals(FILES))
            throw new IllegalArgumentException("Expected html, css and javascript files only");
        Set<String> editable = new HashSet<>();
        config.path("editableFiles").forEach(file -> editable.add(file.asText()));
        var output = JSON.createObjectNode();
        for (String file : FILES) {
            requireText(input, file);
            if (!editable.contains(file)
                    && !input.path(file).asText().equals(config.path(file).asText())) {
                throw new IllegalArgumentException("This exercise does not allow editing " + file);
            }
            output.put(file, input.path(file).asText());
        }
        return output.toString();
    }

    private static JsonNode parse(String value) {
        try {
            if (value == null || value.length() > 200_000)
                throw new IllegalArgumentException("Invalid web source size");
            JsonNode node = JSON.readTree(value);
            if (node == null || !node.isObject())
                throw new IllegalArgumentException("Expected a web source object");
            return node;
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Invalid web source JSON", exception);
        }
    }

    private static void requireText(JsonNode node, String field) {
        if (!node.path(field).isTextual())
            throw new IllegalArgumentException(field + " must be a string");
    }

    private WebExerciseSource() {}
}
