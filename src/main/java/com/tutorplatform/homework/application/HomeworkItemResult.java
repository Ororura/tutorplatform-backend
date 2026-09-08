package com.tutorplatform.homework.application;

import java.util.UUID;

public record HomeworkItemResult(
        UUID id,
        UUID taskId,
        String taskTitle,
        int position,
        boolean required
) {
}
