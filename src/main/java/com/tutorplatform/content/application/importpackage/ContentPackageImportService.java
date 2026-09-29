package com.tutorplatform.content.application.importpackage;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.application.CreateLessonMaterialCommand;
import com.tutorplatform.content.application.LessonMaterialService;
import com.tutorplatform.program.api.CreateLearningProgramModuleRequest;
import com.tutorplatform.program.api.CreateLearningProgramTopicRequest;
import com.tutorplatform.program.application.TeacherLearningProgramService;
import com.tutorplatform.task.application.AttachTaskToTopicCommand;
import com.tutorplatform.task.application.CreateTaskCommand;
import com.tutorplatform.task.application.ProgrammingTaskConfigInput;
import com.tutorplatform.task.application.TaskService;
import com.tutorplatform.task.application.TaskTestCaseInput;
import com.tutorplatform.task.domain.programming.ComparisonMode;
import com.tutorplatform.task.domain.programming.ProgrammingLanguage;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentPackageImportService {
    private final TeacherLearningProgramService programs;
    private final ContentPackagePreviewService preview;
    private final LessonMaterialService materials;
    private final TaskService tasks;
    private final ContentPackageParser parser;
    private final ContentPackageImportRepository imports;

    public ContentPackageImportService(
            TeacherLearningProgramService programs,
            ContentPackagePreviewService preview,
            LessonMaterialService materials,
            TaskService tasks,
            ContentPackageParser parser,
            ContentPackageImportRepository imports) {
        this.programs = programs;
        this.preview = preview;
        this.materials = materials;
        this.tasks = tasks;
        this.parser = parser;
        this.imports = imports;
    }

    @Transactional
    public ContentPackageImportResult importPackage(
            AuthenticatedUser principal,
            UUID programId,
            UUID confirmationId,
            String expectedDigest,
            byte[] yamlBytes) {
        UUID teacherId = programs.requireEditableForImport(principal, programId);
        if (confirmationId == null) {
            throw new IllegalArgumentException("confirmationId is required");
        }

        // Preview reparses and validates the bytes and produces the exact values manual create
        // requests must store. It hashes the original bytes, not a serialized package model.
        ContentPackagePreviewResult packagePreview =
                preview.preview(principal, programId, yamlBytes);
        String digest = packagePreview.sha256Digest();
        if (!digest.equals(expectedDigest)) {
            throw new ContentPackageDigestMismatchException();
        }

        var existing = imports.registerConfirmation(teacherId, programId, confirmationId);
        if (existing.isPresent()) {
            if (!digest.equals(existing.get().packageDigest())) {
                throw new ContentPackageConfirmationConflictException();
            }
            return ContentPackageImportResult.fromRecord(existing.get(), true);
        }

        // Preview validates the bytes and hides testcase bodies. Parse the same validated bytes
        // only for v2 persistence; no testcase content enters the API preview or import result.
        TutorContentPackage contentPackage =
                packagePreview.schemaVersion() == 2 ? parser.parse(yamlBytes) : null;
        UUID subjectId =
                contentPackage == null ? null : programs.get(principal, programId).subject().id();
        var moduleIds = new ArrayList<UUID>();
        for (int moduleIndex = 0; moduleIndex < packagePreview.modules().size(); moduleIndex++) {
            var module = packagePreview.modules().get(moduleIndex);
            UUID moduleId =
                    programs.createModule(
                                    principal,
                                    programId,
                                    new CreateLearningProgramModuleRequest(
                                            module.title(), module.description()))
                            .id();
            moduleIds.add(moduleId);
            for (int topicIndex = 0; topicIndex < module.topics().size(); topicIndex++) {
                var topic = module.topics().get(topicIndex);
                UUID topicId =
                        programs.createTopic(
                                        principal,
                                        programId,
                                        moduleId,
                                        new CreateLearningProgramTopicRequest(
                                                topic.title(), topic.description()))
                                .id();
                for (int position = 0; position < topic.materials().size(); position++) {
                    var material = topic.materials().get(position);
                    materials.createLessonMaterial(
                            principal,
                            new CreateLessonMaterialCommand(
                                    topicId,
                                    material.materialType(),
                                    material.title(),
                                    material.content(),
                                    null,
                                    material.externalUrl(),
                                    position));
                }
                if (contentPackage != null) {
                    List<TaskImport> topicTasks =
                            contentPackage
                                    .modules()
                                    .get(moduleIndex)
                                    .topics()
                                    .get(topicIndex)
                                    .tasks();
                    for (int position = 0; position < topicTasks.size(); position++) {
                        TaskImport task = topicTasks.get(position);
                        UUID taskId =
                                tasks.createTask(principal, toCreateTaskCommand(subjectId, task))
                                        .id();
                        tasks.attachTaskToTopic(
                                principal,
                                topicId,
                                taskId,
                                new AttachTaskToTopicCommand(position, task.required()));
                    }
                }
            }
        }

        return ContentPackageImportResult.fromRecord(
                imports.saveSuccessfulImport(
                        teacherId,
                        programId,
                        confirmationId,
                        digest,
                        packagePreview.moduleCount(),
                        packagePreview.topicCount(),
                        packagePreview.materialCount(),
                        packagePreview.taskCount(),
                        moduleIds));
    }

    private static CreateTaskCommand toCreateTaskCommand(UUID subjectId, TaskImport task) {
        ProgrammingConfigImport config = task.programmingConfig();
        List<TaskTestCaseInput> testCases =
                task.testCases() == null
                        ? null
                        : java.util.stream.IntStream.range(0, task.testCases().size())
                                .mapToObj(
                                        position -> {
                                            TestCaseImport testCase =
                                                    task.testCases().get(position);
                                            return new TaskTestCaseInput(
                                                    null,
                                                    testCase.inputText(),
                                                    testCase.expectedOutput(),
                                                    testCase.hidden(),
                                                    ComparisonMode.valueOf(
                                                            testCase.comparisonMode()),
                                                    position);
                                        })
                                .toList();
        return new CreateTaskCommand(
                subjectId,
                task.title(),
                task.descriptionMarkdown(),
                TaskDifficulty.valueOf(task.difficulty()),
                TaskType.valueOf(task.taskType()),
                config == null
                        ? null
                        : new ProgrammingTaskConfigInput(
                                ProgrammingLanguage.valueOf(config.language()),
                                config.starterCode(),
                                config.executionEnabled(),
                                config.timeLimitMs(),
                                config.memoryLimitMb()),
                testCases);
    }
}
