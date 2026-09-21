package com.tutorplatform.session.application;

import java.util.List;

public record LessonSessionPageResult(
        List<LessonSessionResult> items, int page, int size, long totalElements, int totalPages) {}
