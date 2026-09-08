package com.tutorplatform.program.application;

import com.tutorplatform.program.domain.ModuleRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import com.tutorplatform.program.domain.TopicRepository;
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
                .flatMap(topic -> moduleRepository.findById(topic.getModuleId()))
                .flatMap(module -> learningProgramRepository.findById(module.getLearningProgramId()))
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
                        .findById(studentProgram.getLearningProgramId())
                        .map(learningProgram -> new StudentProgramContext(
                                studentProgram.getId(),
                                studentProgram.getStudentId(),
                                studentProgram.getLearningProgramId(),
                                studentProgram.getAssignedByTeacherId(),
                                learningProgram.getSubjectId()
                        )));
    }

    @Override
    public boolean topicBelongsToLearningProgram(UUID topicId, UUID learningProgramId) {
        return topicRepository.findById(topicId)
                .flatMap(topic -> moduleRepository.findById(topic.getModuleId()))
                .map(module -> module.getLearningProgramId().equals(learningProgramId))
                .orElse(false);
    }
}
