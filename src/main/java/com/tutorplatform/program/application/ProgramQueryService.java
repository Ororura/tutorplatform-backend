package com.tutorplatform.program.application;

import com.tutorplatform.program.domain.ModuleRepository;
import com.tutorplatform.program.domain.TopicRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProgramQueryService implements ProgramQuery {

    private final StudentProgramRepository studentProgramRepository;
    private final TopicRepository topicRepository;
    private final ModuleRepository moduleRepository;
    private final LearningProgramRepository learningProgramRepository;

    public ProgramQueryService(
        StudentProgramRepository studentProgramRepository,
        TopicRepository topicRepository,
        ModuleRepository moduleRepository,
        LearningProgramRepository learningProgramRepository
    ) {
        this.studentProgramRepository = studentProgramRepository;
        this.topicRepository = topicRepository;
        this.moduleRepository = moduleRepository;
        this.learningProgramRepository = learningProgramRepository;
    }

    @Override
    public Optional<TopicContext> findTopic(UUID topicId) {
        return topicRepository.findById(topicId)
            .flatMap(topic -> moduleRepository.findById(topic.moduleId()))
            .flatMap(module -> learningProgramRepository.findById(module.learningProgramId()))
            .map(learningProgram -> new TopicContext(
                topicId,
                learningProgram.getId(),
                learningProgram.getTeacherId(),
                learningProgram.getSubjectId()
            ));
    }

    @Override
    public Optional<StudentProgramContext> findStudentProgram(UUID studentProgramId) {
        return studentProgramRepository.findById(studentProgramId)
            .flatMap(studentProgram -> learningProgramRepository
                .findById(studentProgram.learningProgramId())
                .map(learningProgram -> new StudentProgramContext(
                    studentProgram.id(),
                    studentProgram.studentId(),
                    studentProgram.learningProgramId(),
                    studentProgram.assignedByTeacherId(),
                    learningProgram.getSubjectId()
                )));
    }

    @Override
    public boolean topicBelongsToLearningProgram(UUID topicId, UUID learningProgramId) {
        return topicRepository.findById(topicId)
            .flatMap(topic -> moduleRepository.findById(topic.moduleId()))
            .map(module -> module.learningProgramId().equals(learningProgramId))
            .orElse(false);
    }
}
