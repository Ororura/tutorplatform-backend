package com.tutorplatform.content.application.importpackage;

import com.tutorplatform.content.application.importpackage.ContentPackageValidationError.Code;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Validates the package contract after YAML has been parsed into import DTOs. */
@Component
public final class TutorContentPackageValidator {
    private static final int MAX_MODULES = 15;
    private static final int MAX_TOPICS = 25;
    private static final int MAX_MATERIALS = 250;
    private static final int MAX_TASKS = 250;
    private static final int MAX_TEST_CASES_PER_TASK = 100;

    public ContentPackageValidationResult validate(TutorContentPackage contentPackage) {
        List<ContentPackageValidationError> errors = new ArrayList<>();
        if (contentPackage == null) {
            add(errors, Code.REQUIRED_FIELD, "root", "Package is required");
            return new ContentPackageValidationResult(errors);
        }
        if (contentPackage.schemaVersion() == null) {
            add(errors, Code.REQUIRED_FIELD, "schemaVersion", "Schema version is required");
        } else if (contentPackage.schemaVersion() != 1 && contentPackage.schemaVersion() != 2) {
            add(
                    errors,
                    Code.UNSUPPORTED_SCHEMA_VERSION,
                    "schemaVersion",
                    "Only schema versions 1 and 2 are supported");
        }
        if (contentPackage.kind() == null || contentPackage.kind().isBlank()) {
            add(errors, Code.REQUIRED_FIELD, "kind", "Package kind is required");
        } else if (!"modules".equals(contentPackage.kind())) {
            add(errors, Code.INVALID_PACKAGE_KIND, "kind", "Only modules packages are supported");
        }
        List<ModuleImport> modules = contentPackage.modules();
        if (modules == null || modules.isEmpty()) {
            add(errors, Code.REQUIRED_FIELD, "modules", "At least one module is required");
            return new ContentPackageValidationResult(errors);
        }
        if (modules.size() > MAX_MODULES) {
            add(errors, Code.LIMIT_EXCEEDED, "modules", "A package supports at most 15 modules");
        }
        int materialCount = 0;
        int validatedTaskCount = 0;
        boolean taskLimitReported = false;
        for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
            String modulePath = "modules[" + moduleIndex + "]";
            ModuleImport module = modules.get(moduleIndex);
            if (module == null) {
                add(errors, Code.REQUIRED_FIELD, modulePath, "Module is required");
                continue;
            }
            title(errors, module.title(), modulePath + ".title", 180, true);
            List<TopicImport> topics = module.topics();
            if (topics == null || topics.isEmpty()) {
                add(
                        errors,
                        Code.REQUIRED_FIELD,
                        modulePath + ".topics",
                        "At least one topic is required");
                continue;
            }
            if (topics.size() > MAX_TOPICS) {
                add(
                        errors,
                        Code.LIMIT_EXCEEDED,
                        modulePath + ".topics",
                        "A module supports at most 25 topics");
            }
            for (int topicIndex = 0; topicIndex < topics.size(); topicIndex++) {
                String topicPath = modulePath + ".topics[" + topicIndex + "]";
                TopicImport topic = topics.get(topicIndex);
                if (topic == null) {
                    add(errors, Code.REQUIRED_FIELD, topicPath, "Topic is required");
                    continue;
                }
                title(errors, topic.title(), topicPath + ".title", 180, true);
                List<MaterialImport> materials = topic.materials();
                if (materials != null) {
                    materialCount += materials.size();
                    for (int materialIndex = 0; materialIndex < materials.size(); materialIndex++) {
                        String materialPath = topicPath + ".materials[" + materialIndex + "]";
                        MaterialImport material = materials.get(materialIndex);
                        if (material == null) {
                            add(errors, Code.REQUIRED_FIELD, materialPath, "Material is required");
                            continue;
                        }
                        validateMaterial(errors, material, materialPath);
                    }
                }
                List<TaskImport> tasks = topic.tasks();
                if (tasks == null || tasks.isEmpty()) continue;
                String tasksPath = topicPath + ".tasks";
                if (contentPackage.schemaVersion() == 1) {
                    add(
                            errors,
                            Code.FORBIDDEN_FIELD,
                            tasksPath,
                            "Tasks are not allowed in schema version 1");
                } else if (contentPackage.schemaVersion() == 2) {
                    int remaining = MAX_TASKS - validatedTaskCount;
                    if (tasks.size() > remaining && !taskLimitReported) {
                        add(
                                errors,
                                Code.LIMIT_EXCEEDED,
                                tasksPath,
                                "A package supports at most 250 tasks");
                        taskLimitReported = true;
                    }
                    int taskCount = Math.min(tasks.size(), remaining);
                    for (int taskIndex = 0; taskIndex < taskCount; taskIndex++) {
                        String taskPath = tasksPath + "[" + taskIndex + "]";
                        TaskImport task = tasks.get(taskIndex);
                        if (task == null) {
                            add(errors, Code.REQUIRED_FIELD, taskPath, "Task is required");
                            continue;
                        }
                        validateTask(errors, task, taskPath);
                    }
                    validatedTaskCount += taskCount;
                }
            }
        }
        if (materialCount > MAX_MATERIALS) {
            add(errors, Code.LIMIT_EXCEEDED, "modules", "A package supports at most 250 materials");
        }
        return new ContentPackageValidationResult(errors);
    }

    private void validateTask(
            List<ContentPackageValidationError> errors, TaskImport task, String path) {
        title(errors, task.title(), path + ".title", 220, true);
        if (task.descriptionMarkdown() == null) {
            add(
                    errors,
                    Code.REQUIRED_FIELD,
                    path + ".descriptionMarkdown",
                    "Description is required");
        }
        if (task.required() == null) {
            add(errors, Code.REQUIRED_FIELD, path + ".required", "Required flag is required");
        }
        String difficulty = task.difficulty();
        if (difficulty == null || difficulty.isBlank()) {
            add(errors, Code.REQUIRED_FIELD, path + ".difficulty", "Difficulty is required");
        } else if (!difficulty.equals("EASY")
                && !difficulty.equals("MEDIUM")
                && !difficulty.equals("HARD")) {
            add(errors, Code.INVALID_FIELD_VALUE, path + ".difficulty", "Unsupported difficulty");
        }
        String type = task.taskType();
        if (type == null || type.isBlank()) {
            add(errors, Code.REQUIRED_FIELD, path + ".taskType", "Task type is required");
        } else if (type.equals("TEXT")) {
            if (task.programmingConfig() != null) {
                add(
                        errors,
                        Code.FORBIDDEN_FIELD,
                        path + ".programmingConfig",
                        "Programming config is not allowed for TEXT tasks");
            }
            if (task.testCases() != null) {
                add(
                        errors,
                        Code.FORBIDDEN_FIELD,
                        path + ".testCases",
                        "Test cases are not allowed for TEXT tasks");
            }
        } else if (type.equals("CODE")) {
            validateCodeTask(errors, task, path);
        } else {
            add(errors, Code.INVALID_FIELD_VALUE, path + ".taskType", "Unsupported task type");
        }
    }

    private void validateCodeTask(
            List<ContentPackageValidationError> errors, TaskImport task, String path) {
        String configPath = path + ".programmingConfig";
        ProgrammingConfigImport config = task.programmingConfig();
        if (config == null) {
            add(errors, Code.REQUIRED_FIELD, configPath, "Programming config is required");
        } else {
            String language = config.language();
            if (language == null || language.isBlank()) {
                add(errors, Code.REQUIRED_FIELD, configPath + ".language", "Language is required");
            } else if (!language.equals("PYTHON")) {
                add(
                        errors,
                        Code.INVALID_FIELD_VALUE,
                        configPath + ".language",
                        "Unsupported language");
            }
            if (config.executionEnabled() == null) {
                add(
                        errors,
                        Code.REQUIRED_FIELD,
                        configPath + ".executionEnabled",
                        "Execution enabled flag is required");
            }
            checkRange(errors, config.timeLimitMs(), configPath + ".timeLimitMs", 100, 30_000);
            checkRange(errors, config.memoryLimitMb(), configPath + ".memoryLimitMb", 16, 1_024);
        }
        String testCasesPath = path + ".testCases";
        List<TestCaseImport> testCases = task.testCases();
        if (testCases == null || testCases.isEmpty()) {
            add(errors, Code.REQUIRED_FIELD, testCasesPath, "At least one test case is required");
            return;
        }
        if (testCases.size() > MAX_TEST_CASES_PER_TASK) {
            add(
                    errors,
                    Code.LIMIT_EXCEEDED,
                    testCasesPath,
                    "A CODE task supports at most 100 test cases");
        }
        for (int testCaseIndex = 0;
                testCaseIndex < Math.min(testCases.size(), MAX_TEST_CASES_PER_TASK);
                testCaseIndex++) {
            String testCasePath = testCasesPath + "[" + testCaseIndex + "]";
            TestCaseImport testCase = testCases.get(testCaseIndex);
            if (testCase == null) {
                add(errors, Code.REQUIRED_FIELD, testCasePath, "Test case is required");
                continue;
            }
            if (testCase.expectedOutput() == null) {
                add(
                        errors,
                        Code.REQUIRED_FIELD,
                        testCasePath + ".expectedOutput",
                        "Expected output is required");
            }
            if (testCase.hidden() == null) {
                add(
                        errors,
                        Code.REQUIRED_FIELD,
                        testCasePath + ".hidden",
                        "Hidden flag is required");
            }
            String comparisonMode = testCase.comparisonMode();
            if (comparisonMode == null || comparisonMode.isBlank()) {
                add(
                        errors,
                        Code.REQUIRED_FIELD,
                        testCasePath + ".comparisonMode",
                        "Comparison mode is required");
            } else if (!comparisonMode.equals("EXACT") && !comparisonMode.equals("NORMALIZED")) {
                add(
                        errors,
                        Code.INVALID_FIELD_VALUE,
                        testCasePath + ".comparisonMode",
                        "Unsupported comparison mode");
            }
        }
    }

    private static void checkRange(
            List<ContentPackageValidationError> errors,
            Integer value,
            String path,
            int minimum,
            int maximum) {
        if (value == null) {
            add(errors, Code.REQUIRED_FIELD, path, "Value is required");
        } else if (value < minimum || value > maximum) {
            add(
                    errors,
                    Code.INVALID_FIELD_VALUE,
                    path,
                    "Value must be between " + minimum + " and " + maximum);
        }
    }

    private void validateMaterial(
            List<ContentPackageValidationError> errors, MaterialImport material, String path) {
        title(errors, material.title(), path + ".title", 200, false);
        String type = material.materialType();
        if (type == null || type.isBlank()) {
            add(errors, Code.REQUIRED_FIELD, path + ".materialType", "Material type is required");
            return;
        }
        switch (type) {
            case "TEXT", "MARKDOWN", "CODE_EXAMPLE" -> {
                if (material.content() == null || material.content().isBlank()) {
                    add(
                            errors,
                            Code.REQUIRED_FIELD,
                            path + ".content",
                            "Content is required for " + type + " material");
                }
                if (material.externalUrl() != null) {
                    add(
                            errors,
                            Code.FORBIDDEN_FIELD,
                            path + ".externalUrl",
                            "External URL is not allowed for " + type + " material");
                }
            }
            case "LINK" -> {
                if (material.externalUrl() == null || material.externalUrl().isBlank()) {
                    add(
                            errors,
                            Code.REQUIRED_FIELD,
                            path + ".externalUrl",
                            "External URL is required for LINK material");
                } else if (!validHttpUrl(material.externalUrl())) {
                    add(
                            errors,
                            Code.INVALID_EXTERNAL_URL,
                            path + ".externalUrl",
                            "External URL must be an absolute HTTP or HTTPS URL with a host");
                }
                if (material.content() != null) {
                    add(
                            errors,
                            Code.FORBIDDEN_FIELD,
                            path + ".content",
                            "Content is not allowed for LINK material");
                }
            }
            default ->
                    add(
                            errors,
                            Code.INVALID_MATERIAL_TYPE,
                            path + ".materialType",
                            "Unsupported material type");
        }
    }

    private static void title(
            List<ContentPackageValidationError> errors,
            String value,
            String path,
            int maxLength,
            boolean strip) {
        if (value == null || value.isBlank()) {
            add(errors, Code.REQUIRED_FIELD, path, "Title is required");
        } else if ((strip ? value.strip() : value).length() > maxLength) {
            add(errors, Code.LIMIT_EXCEEDED, path, "Title exceeds " + maxLength + " characters");
        }
    }

    private static boolean validHttpUrl(String value) {
        try {
            URI uri = new URI(value);
            return uri.isAbsolute()
                    && ("http".equalsIgnoreCase(uri.getScheme())
                            || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    private static void add(
            List<ContentPackageValidationError> errors, Code code, String path, String message) {
        errors.add(new ContentPackageValidationError(code, path, message));
    }
}
