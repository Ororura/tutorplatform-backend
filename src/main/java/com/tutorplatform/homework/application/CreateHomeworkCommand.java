package com.tutorplatform.homework.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateHomeworkCommand(
    UUID studentId,
    UUID studentProgramId,
    String title,
    String description,
    Instant dueAt,
    List<HomeworkItemInput> items
) {
}
