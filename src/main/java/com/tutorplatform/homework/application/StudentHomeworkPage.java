package com.tutorplatform.homework.application;

import java.util.List;

public record StudentHomeworkPage(
    List<StudentHomeworkListItem> items,
    long totalElements,
    int totalPages
) {
}
