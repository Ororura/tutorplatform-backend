package com.tutorplatform.homework.application;

import java.util.List;

public record HomeworkPage(
    List<HomeworkListItem> items,
    long totalElements,
    int totalPages
) {
}
