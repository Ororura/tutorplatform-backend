package com.tutorplatform.homework.application;

import com.tutorplatform.homework.domain.HomeworkStatus;
import java.time.Instant;
import java.util.UUID;

public record StudentHomeworkSummaryResult(
        UUID id,
        UUID studentProgramId,
        String title,
        HomeworkStatus status,
        Instant assignedAt,
        Instant dueAt,
        boolean overdue,
        Instant completedAt,
        long itemsCount,
        Instant createdAt) {}
