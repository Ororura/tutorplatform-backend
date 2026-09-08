package com.tutorplatform.homework.application;

import java.time.Instant;
import java.util.List;

public record UpdateHomeworkCommand(
        String title,
        String description,
        Instant dueAt,
        Long version,
        List<HomeworkItemInput> items
) {
}
