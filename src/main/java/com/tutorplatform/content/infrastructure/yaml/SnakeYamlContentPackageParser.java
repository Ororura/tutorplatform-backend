package com.tutorplatform.content.infrastructure.yaml;

import com.tutorplatform.content.application.importpackage.ContentPackageParseException;
import com.tutorplatform.content.application.importpackage.ContentPackageParseException.Code;
import com.tutorplatform.content.application.importpackage.ContentPackageParser;
import com.tutorplatform.content.application.importpackage.MaterialImport;
import com.tutorplatform.content.application.importpackage.ModuleImport;
import com.tutorplatform.content.application.importpackage.ProgrammingConfigImport;
import com.tutorplatform.content.application.importpackage.TaskImport;
import com.tutorplatform.content.application.importpackage.TestCaseImport;
import com.tutorplatform.content.application.importpackage.TopicImport;
import com.tutorplatform.content.application.importpackage.TutorContentPackage;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.events.AliasEvent;
import org.yaml.snakeyaml.events.CollectionStartEvent;
import org.yaml.snakeyaml.events.DocumentStartEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.NodeEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.nodes.Tag;

/** Parses untrusted package bytes as YAML nodes, without constructing Java beans. */
@Component
public final class SnakeYamlContentPackageParser implements ContentPackageParser {
    private static final int MAX_BYTES = 1_048_576;
    private static final int MAX_DEPTH = 9;

    @Override
    public TutorContentPackage parse(byte[] yamlBytes) {
        if (yamlBytes == null) {
            throw error(Code.INVALID_YAML, "root", "YAML input is missing");
        }
        if (yamlBytes.length > MAX_BYTES) {
            throw error(Code.FILE_TOO_LARGE, "root", "YAML file exceeds 1 MiB");
        }
        String source;
        try {
            source =
                    StandardCharsets.UTF_8
                            .newDecoder()
                            .decode(ByteBuffer.wrap(yamlBytes))
                            .toString();
        } catch (CharacterCodingException ex) {
            throw error(Code.INVALID_UTF8, "root", "Input must be valid UTF-8");
        }
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        options.setMaxAliasesForCollections(0);
        options.setNestingDepthLimit(MAX_DEPTH);
        options.setCodePointLimit(MAX_BYTES);
        Yaml yaml = new Yaml(options);
        try {
            rejectFeatures(yaml, source);
            var documents = yaml.composeAll(new StringReader(source)).iterator();
            if (!documents.hasNext()) {
                throw error(Code.INVALID_YAML, "root", "Exactly one YAML document is required");
            }
            Node root = documents.next();
            if (documents.hasNext()) {
                throw error(Code.INVALID_YAML, "root", "Multiple YAML documents are not supported");
            }
            return readPackage(root);
        } catch (ContentPackageParseException ex) {
            throw ex;
        } catch (YAMLException ex) {
            throw error(Code.INVALID_YAML, "root", "Invalid YAML syntax or nesting");
        }
    }

    private void rejectFeatures(Yaml yaml, String source) {
        int documents = 0;
        int depth = 0;
        for (Event event : yaml.parse(new StringReader(source))) {
            if (event instanceof DocumentStartEvent) {
                if (++documents > 1) {
                    throw error(
                            Code.INVALID_YAML,
                            location(event.getStartMark()),
                            "Multiple YAML documents are not supported");
                }
            }
            if (event instanceof AliasEvent
                    || event instanceof NodeEvent node && node.getAnchor() != null) {
                throw error(
                        Code.UNSUPPORTED_YAML_FEATURE,
                        location(event.getStartMark()),
                        "YAML anchors and aliases are not supported");
            }
            if (event instanceof ScalarEvent scalar && scalar.getTag() != null
                    || event instanceof CollectionStartEvent collection
                            && collection.getTag() != null) {
                throw error(
                        Code.UNSUPPORTED_YAML_FEATURE,
                        location(event.getStartMark()),
                        "Explicit YAML tags are not supported");
            }
            if (event instanceof CollectionStartEvent) {
                if (++depth > MAX_DEPTH) {
                    throw error(
                            Code.UNSUPPORTED_YAML_FEATURE,
                            location(event.getStartMark()),
                            "YAML nesting is too deep");
                }
            } else if (event.is(Event.ID.MappingEnd) || event.is(Event.ID.SequenceEnd)) {
                depth--;
            }
        }
    }

    private TutorContentPackage readPackage(Node root) {
        Map<String, Node> fields = fields(root, "root", Set.of("schemaVersion", "kind", "modules"));
        return new TutorContentPackage(
                integer(fields.get("schemaVersion"), "schemaVersion"),
                string(fields.get("kind"), "kind"),
                modules(fields.get("modules"), "modules"));
    }

    private List<ModuleImport> modules(Node node, String path) {
        if (node == null) return null;
        List<ModuleImport> result = new ArrayList<>();
        int index = 0;
        for (Node item : sequence(node, path)) {
            String itemPath = path + "[" + index++ + "]";
            Map<String, Node> fields =
                    fields(item, itemPath, Set.of("title", "description", "topics"));
            result.add(
                    new ModuleImport(
                            string(fields.get("title"), itemPath + ".title"),
                            string(fields.get("description"), itemPath + ".description"),
                            topics(fields.get("topics"), itemPath + ".topics")));
        }
        return result;
    }

    private List<TopicImport> topics(Node node, String path) {
        if (node == null) return null;
        List<TopicImport> result = new ArrayList<>();
        int index = 0;
        for (Node item : sequence(node, path)) {
            String itemPath = path + "[" + index++ + "]";
            Map<String, Node> fields =
                    fields(item, itemPath, Set.of("title", "description", "materials", "tasks"));
            result.add(
                    new TopicImport(
                            string(fields.get("title"), itemPath + ".title"),
                            string(fields.get("description"), itemPath + ".description"),
                            materials(fields.get("materials"), itemPath + ".materials"),
                            tasks(fields.get("tasks"), itemPath + ".tasks")));
        }
        return result;
    }

    private List<MaterialImport> materials(Node node, String path) {
        if (node == null) return List.of();
        List<MaterialImport> result = new ArrayList<>();
        int index = 0;
        for (Node item : sequence(node, path)) {
            String itemPath = path + "[" + index++ + "]";
            Map<String, Node> fields =
                    fields(
                            item,
                            itemPath,
                            Set.of("title", "materialType", "content", "externalUrl"));
            result.add(
                    new MaterialImport(
                            string(fields.get("title"), itemPath + ".title"),
                            string(fields.get("materialType"), itemPath + ".materialType"),
                            string(fields.get("content"), itemPath + ".content"),
                            string(fields.get("externalUrl"), itemPath + ".externalUrl")));
        }
        return result;
    }

    private List<TaskImport> tasks(Node node, String path) {
        if (node == null) return List.of();
        List<TaskImport> result = new ArrayList<>();
        int index = 0;
        for (Node item : sequence(node, path)) {
            String itemPath = path + "[" + index++ + "]";
            Map<String, Node> fields =
                    fields(
                            item,
                            itemPath,
                            Set.of(
                                    "title",
                                    "descriptionMarkdown",
                                    "taskType",
                                    "difficulty",
                                    "required",
                                    "programmingConfig",
                                    "testCases"));
            result.add(
                    new TaskImport(
                            string(fields.get("title"), itemPath + ".title"),
                            string(
                                    fields.get("descriptionMarkdown"),
                                    itemPath + ".descriptionMarkdown"),
                            string(fields.get("taskType"), itemPath + ".taskType"),
                            string(fields.get("difficulty"), itemPath + ".difficulty"),
                            bool(fields.get("required"), itemPath + ".required"),
                            programmingConfig(
                                    fields.get("programmingConfig"),
                                    itemPath + ".programmingConfig"),
                            testCases(fields.get("testCases"), itemPath + ".testCases")));
        }
        return result;
    }

    private ProgrammingConfigImport programmingConfig(Node node, String path) {
        if (node == null) return null;
        Map<String, Node> fields =
                fields(
                        node,
                        path,
                        Set.of(
                                "language",
                                "starterCode",
                                "executionEnabled",
                                "timeLimitMs",
                                "memoryLimitMb"));
        return new ProgrammingConfigImport(
                string(fields.get("language"), path + ".language"),
                string(fields.get("starterCode"), path + ".starterCode"),
                bool(fields.get("executionEnabled"), path + ".executionEnabled"),
                integer(fields.get("timeLimitMs"), path + ".timeLimitMs"),
                integer(fields.get("memoryLimitMb"), path + ".memoryLimitMb"));
    }

    private List<TestCaseImport> testCases(Node node, String path) {
        if (node == null) return List.of();
        List<TestCaseImport> result = new ArrayList<>();
        int index = 0;
        for (Node item : sequence(node, path)) {
            String itemPath = path + "[" + index++ + "]";
            Map<String, Node> fields =
                    fields(
                            item,
                            itemPath,
                            Set.of("inputText", "expectedOutput", "hidden", "comparisonMode"));
            result.add(
                    new TestCaseImport(
                            string(fields.get("inputText"), itemPath + ".inputText"),
                            string(fields.get("expectedOutput"), itemPath + ".expectedOutput"),
                            bool(fields.get("hidden"), itemPath + ".hidden"),
                            string(fields.get("comparisonMode"), itemPath + ".comparisonMode")));
        }
        return result;
    }

    private Map<String, Node> fields(Node node, String path, Set<String> allowed) {
        if (!(node instanceof MappingNode mapping)) {
            throw error(Code.INVALID_FIELD_TYPE, path, "Expected a mapping");
        }
        Map<String, Node> result = new HashMap<>();
        for (NodeTuple entry : mapping.getValue()) {
            if (!(entry.getKeyNode() instanceof ScalarNode key) || !Tag.STR.equals(key.getTag())) {
                throw error(Code.INVALID_FIELD_TYPE, path, "Field name must be a string");
            }
            String name = key.getValue();
            String fieldPath = path.equals("root") ? name : path + "." + name;
            if (result.containsKey(name)) {
                throw error(Code.DUPLICATE_KEY, fieldPath, "Duplicate YAML key");
            }
            if (!allowed.contains(name)) {
                throw error(Code.UNKNOWN_FIELD, fieldPath, "Unknown field");
            }
            result.put(name, entry.getValueNode());
        }
        return result;
    }

    private List<Node> sequence(Node node, String path) {
        if (!(node instanceof SequenceNode sequence)) {
            throw error(Code.INVALID_FIELD_TYPE, path, "Expected an array");
        }
        return sequence.getValue();
    }

    private String string(Node node, String path) {
        if (node == null) return null;
        if (!(node instanceof ScalarNode scalar) || !Tag.STR.equals(scalar.getTag())) {
            throw error(Code.INVALID_FIELD_TYPE, path, "Expected a string");
        }
        return scalar.getValue();
    }

    private Integer integer(Node node, String path) {
        if (node == null) return null;
        if (!(node instanceof ScalarNode scalar) || !Tag.INT.equals(scalar.getTag())) {
            throw error(Code.INVALID_FIELD_TYPE, path, "Expected an integer");
        }
        try {
            return Integer.valueOf(scalar.getValue().replace("_", ""));
        } catch (NumberFormatException ex) {
            throw error(Code.INVALID_FIELD_TYPE, path, "Expected a 32-bit decimal integer");
        }
    }

    private Boolean bool(Node node, String path) {
        if (node == null) return null;
        if (!(node instanceof ScalarNode scalar)
                || !Tag.BOOL.equals(scalar.getTag())
                || !("true".equals(scalar.getValue()) || "false".equals(scalar.getValue()))) {
            throw error(Code.INVALID_FIELD_TYPE, path, "Expected a boolean");
        }
        return Boolean.valueOf(scalar.getValue());
    }

    private static String location(Mark mark) {
        return "line " + (mark.getLine() + 1) + ", column " + (mark.getColumn() + 1);
    }

    private static ContentPackageParseException error(Code code, String location, String message) {
        return new ContentPackageParseException(code, location, message);
    }
}
