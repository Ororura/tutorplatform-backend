package com.tutorplatform.homework.application;

import com.tutorplatform.homework.domain.HomeworkStatus;
import java.time.Instant;
import java.util.UUID;

public record HomeworkListItem(
        UUID id,
        UUID studentProgramId,
        String title,
        HomeworkStatus status,
        Instant assignedAt,
        Instant dueAt,
        Instant completedAt,
        Instant createdAt) {}
