package com.tutorplatform.program.application;

import com.tutorplatform.program.domain.TopicStatus;
import com.tutorplatform.program.domain.studentprogram.StudentProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeacherStudentProgramQuery {

    List<StudentProgramSummary> findPrograms(UUID teacherId, UUID studentId);

    Optional<StudentProgramDetails> findProgram(
            UUID teacherId, UUID studentId, UUID studentProgramId);

    List<StudentProgramSummary> findProgramsByStudentId(UUID studentId);

    Optional<StudentProgramDetails> findProgramByStudentId(UUID studentId, UUID studentProgramId);

    record ProgramSubject(UUID id, String code, String name) {}

    record StudentProgramSummary(
            UUID id,
            UUID learningProgramId,
            String title,
            String description,
            StudentProgramStatus status,
            int reportIntervalMinutes,
            Instant startedAt,
            Instant completedAt,
            ProgramSubject subject) {}

    record StudentProgramDetails(
            UUID id,
            UUID learningProgramId,
            String title,
            String description,
            StudentProgramStatus status,
            int reportIntervalMinutes,
            Instant startedAt,
            Instant completedAt,
            ProgramSubject subject,
            List<ProgramModule> modules) {}

    record ProgramModule(
            UUID id, String title, String description, int position, List<ProgramTopic> topics) {}

    record ProgramTopic(
            UUID id,
            String title,
            String description,
            int position,
            TopicStatus topicStatus,
            StudentTopicProgressStatus progressStatus) {}
}
