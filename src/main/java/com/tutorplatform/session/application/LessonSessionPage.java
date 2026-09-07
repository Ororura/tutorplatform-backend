package com.tutorplatform.session.application;

import com.tutorplatform.session.domain.LessonSessionEntity;

import java.util.List;

public record LessonSessionPage(
    List<LessonSessionEntity> items,
    long totalElements,
    int totalPages
) {
}
