package com.tutorplatform.dashboard.application;

import java.util.List;

public record TeacherDashboardAttentionPage(
        List<TeacherDashboardResult.AttentionItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {}
