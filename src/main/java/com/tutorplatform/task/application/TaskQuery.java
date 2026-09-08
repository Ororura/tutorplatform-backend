package com.tutorplatform.task.application;

import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskStatus;

import java.util.UUID;

public interface TaskQuery {

    TaskPage findTeacherTextTasks(
            UUID teacherId,
            UUID subjectId,
            TaskStatus status,
            TaskDifficulty difficulty,
            int page,
            int size,
            String sortField,
            boolean ascending
    );
}
