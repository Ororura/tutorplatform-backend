package com.tutorplatform.program.application;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.content.application.LessonMaterialService;
import com.tutorplatform.program.api.StudentLessonMaterialResponse;
import com.tutorplatform.program.api.StudentProgramTopicResponse;
import com.tutorplatform.program.domain.ModuleEntity;
import com.tutorplatform.program.domain.ModuleRepository;
import com.tutorplatform.program.domain.TopicEntity;
import com.tutorplatform.program.domain.TopicRepository;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressRepository;
import com.tutorplatform.student.application.management.StudentNotFoundException;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StudentProgramTopicService {

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final ProgramQuery programQuery;
    private final TopicRepository topicRepository;
    private final ModuleRepository moduleRepository;
    private final StudentTopicProgressRepository progressRepository;
    private final LessonMaterialService lessonMaterialService;

    public StudentProgramTopicService(
        StudentOwnershipQuery studentOwnershipQuery,
        ProgramQuery programQuery,
        TopicRepository topicRepository,
        ModuleRepository moduleRepository,
        StudentTopicProgressRepository progressRepository,
        LessonMaterialService lessonMaterialService
    ) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.programQuery = programQuery;
        this.topicRepository = topicRepository;
        this.moduleRepository = moduleRepository;
        this.progressRepository = progressRepository;
        this.lessonMaterialService = lessonMaterialService;
    }

    public StudentProgramTopicResponse getTopic(
        AuthenticatedUser principal,
        UUID studentProgramId,
        UUID topicId
    ) {
        UUID studentId = studentOwnershipQuery.findStudentIdByUserId(principal.id())
            .orElseThrow(StudentNotFoundException::new);
        ProgramQuery.StudentProgramContext studentProgram = programQuery.findStudentProgram(studentProgramId)
            .filter(program -> program.belongsToStudent(studentId))
            .orElseThrow(StudentProgramNotFoundException::new);

        TopicEntity topic = topicRepository.findById(topicId)
            .orElseThrow(LearningProgramTopicNotFoundException::new);
        ModuleEntity module = moduleRepository.findById(topic.moduleId())
            .filter(value -> value.learningProgramId().equals(studentProgram.learningProgramId()))
            .orElseThrow(LearningProgramTopicNotFoundException::new);

        return new StudentProgramTopicResponse(
            topic.id(),
            topic.title(),
            topic.description(),
            module.id(),
            module.title(),
            progressRepository.findById(studentProgramId, topicId)
                .map(progress -> progress.status())
                .orElse(null),
            lessonMaterialService.listLessonMaterialsForAuthorizedTopic(topicId).stream()
                .map(StudentLessonMaterialResponse::from)
                .toList()
        );
    }
}
