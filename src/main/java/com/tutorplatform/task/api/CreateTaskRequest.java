package com.tutorplatform.task.api;

import com.tutorplatform.task.api.programming.ProgrammingTaskConfigRequest;
import com.tutorplatform.task.api.testcase.TaskTestCaseRequest;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreateTaskRequest(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid")
                UUID subjectId,
        @NotBlank @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 220)
                String title,
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String descriptionMarkdown,
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) TaskDifficulty difficulty,
        @Schema(
                        description = "Defaults to TEXT when omitted",
                        allowableValues = {"TEXT", "CODE"})
                TaskType taskType,
        @Valid ProgrammingTaskConfigRequest programmingConfig,
        List<@Valid TaskTestCaseRequest> testCases) {}
