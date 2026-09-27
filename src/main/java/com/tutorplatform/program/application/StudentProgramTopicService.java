package com.tutorplatform.program.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.application.FileMaterialService;
import com.tutorplatform.content.application.LessonMaterialService;
import com.tutorplatform.program.api.StudentLessonMaterialResponse;
import com.tutorplatform.program.api.StudentProgramTopicResponse;
import com.tutorplatform.program.domain.ModuleEntity;
import com.tutorplatform.program.domain.ModuleRepository;
import com.tutorplatform.program.domain.TopicEntity;
import com.tutorplatform.program.domain.TopicRepository;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressEntity;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressRepository;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressStatus;
import com.tutorplatform.student.application.management.StudentNotFoundException;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentProgramTopicService {

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final ProgramQuery programQuery;
    private final TopicRepository topicRepository;
    private final ModuleRepository moduleRepository;
    private final StudentTopicProgressRepository progressRepository;
    private final LessonMaterialService lessonMaterialService;
    private final FileMaterialService fileMaterialService;

    public StudentProgramTopicService(
            StudentOwnershipQuery studentOwnershipQuery,
            ProgramQuery programQuery,
            TopicRepository topicRepository,
            ModuleRepository moduleRepository,
            StudentTopicProgressRepository progressRepository,
            LessonMaterialService lessonMaterialService,
            FileMaterialService fileMaterialService) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.programQuery = programQuery;
        this.topicRepository = topicRepository;
        this.moduleRepository = moduleRepository;
        this.progressRepository = progressRepository;
        this.lessonMaterialService = lessonMaterialService;
        this.fileMaterialService = fileMaterialService;
    }

    @Transactional
    public StudentProgramTopicResponse getTopic(
            AuthenticatedUser principal, UUID studentProgramId, UUID topicId) {
        AuthorizedTopic authorized = authorizeTopic(principal, studentProgramId, topicId);
        TopicEntity topic = authorized.topic();
        ModuleEntity module = authorized.module();

        StudentTopicProgressEntity progress = requireAccessibleProgress(studentProgramId, topicId);

        return new StudentProgramTopicResponse(
                topic.id(),
                topic.title(),
                topic.description(),
                module.id(),
                module.title(),
                progress.status(),
                lessonMaterialService.listLessonMaterialsForAuthorizedTopic(topicId).stream()
                        .map(StudentLessonMaterialResponse::from)
                        .toList());
    }

    @Transactional
    public FileMaterialService.Download downloadMaterial(
            AuthenticatedUser principal, UUID studentProgramId, UUID topicId, UUID materialId) {
        authorizeTopic(principal, studentProgramId, topicId);

        // A direct download URL must not bypass topic access control.
        // Accessing content of an AVAILABLE topic also starts the topic.
        requireAccessibleProgress(studentProgramId, topicId);

        return fileMaterialService.downloadForAuthorizedTopic(topicId, materialId);
    }

    private StudentTopicProgressEntity requireAccessibleProgress(
            UUID studentProgramId, UUID topicId) {
        List<StudentTopicProgressEntity> rows =
                progressRepository.findAllForUpdate(studentProgramId, List.of(topicId));

        if (rows.size() != 1) {
            throw new LearningProgramTopicNotFoundException();
        }

        StudentTopicProgressEntity progress = rows.getFirst();

        if (progress.status() == StudentTopicProgressStatus.LOCKED) {
            throw new StudentTopicLockedException();
        }

        if (progress.status() != StudentTopicProgressStatus.AVAILABLE) {
            return progress;
        }

        StudentTopicProgressEntity started =
                new StudentTopicProgressEntity(
                        progress.studentProgramId(),
                        progress.topicId(),
                        StudentTopicProgressStatus.IN_PROGRESS,
                        Instant.now(),
                        null,
                        progress.updatedAt());

        return progressRepository.saveAndFlush(started);
    }

    private AuthorizedTopic authorizeTopic(
            AuthenticatedUser principal, UUID studentProgramId, UUID topicId) {
        UUID studentId =
                studentOwnershipQuery
                        .findStudentIdByUserId(principal.id())
                        .orElseThrow(StudentNotFoundException::new);

        ProgramQuery.StudentProgramContext studentProgram =
                programQuery
                        .findStudentProgram(studentProgramId)
                        .filter(program -> program.belongsToStudent(studentId))
                        .orElseThrow(StudentProgramNotFoundException::new);

        TopicEntity topic =
                topicRepository
                        .findById(topicId)
                        .orElseThrow(LearningProgramTopicNotFoundException::new);

        ModuleEntity module =
                moduleRepository
                        .findById(topic.moduleId())
                        .filter(
                                value ->
                                        value.learningProgramId()
                                                .equals(studentProgram.learningProgramId()))
                        .orElseThrow(LearningProgramTopicNotFoundException::new);

        return new AuthorizedTopic(studentProgram, topic, module);
    }

    private record AuthorizedTopic(
            ProgramQuery.StudentProgramContext studentProgram,
            TopicEntity topic,
            ModuleEntity module) {}
}
