package com.tutorplatform.homework.application;

import java.util.UUID;

public record HomeworkItemInput(UUID taskId, int position, boolean required) {}
