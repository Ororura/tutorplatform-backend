package com.tutorplatform.session.application;

import java.time.Instant;
import java.util.UUID;

public record AttendedLessonSession(UUID id, Instant startedAt, int durationMinutes) {}
