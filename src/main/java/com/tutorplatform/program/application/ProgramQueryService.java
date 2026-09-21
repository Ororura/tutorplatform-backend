package com.tutorplatform.program.application;

import com.tutorplatform.program.domain.ModuleRepository;
import com.tutorplatform.program.domain.TopicRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            LearningProgramRepository learningProgramRepository) {
        this.studentProgramRepository = studentProgramRepository;
        this.topicRepository = topicRepository;
        this.moduleRepository = moduleRepository;
        this.learningProgramRepository = learningProgramRepository;
    }

    @Override
    public Optional<TopicContext> findTopic(UUID topicId) {
        return topicRepository
                .findById(topicId)
                .flatMap(topic -> moduleRepository.findById(topic.moduleId()))
                .flatMap(module -> learningProgramRepository.findById(module.learningProgramId()))
                .map(
                        learningProgram ->
                                new TopicContext(
                                        topicId,
                                        learningProgram.getId(),
                                        learningProgram.getTeacherId(),
                                        learningProgram.getSubjectId()));
    }

    @Override
    public Optional<StudentProgramContext> findStudentProgram(UUID studentProgramId) {
        return toContext(studentProgramRepository.findById(studentProgramId));
    }

    @Override
    @Transactional
    public Optional<StudentProgramContext> findStudentProgramForUpdate(UUID studentProgramId) {
        return toContext(studentProgramRepository.findByIdForUpdate(studentProgramId));
    }

    private Optional<StudentProgramContext> toContext(
            Optional<com.tutorplatform.program.domain.studentprogram.StudentProgramEntity> result) {
        return result.flatMap(
                studentProgram ->
                        learningProgramRepository
                                .findById(studentProgram.learningProgramId())
                                .map(
                                        learningProgram ->
                                                new StudentProgramContext(
                                                        studentProgram.id(),
                                                        studentProgram.studentId(),
                                                        studentProgram.learningProgramId(),
                                                        studentProgram.assignedByTeacherId(),
                                                        learningProgram.getSubjectId(),
                                                        studentProgram.reportIntervalMinutes())));
    }

    @Override
    public boolean topicBelongsToLearningProgram(UUID topicId, UUID learningProgramId) {
        return topicRepository
                .findById(topicId)
                .flatMap(topic -> moduleRepository.findById(topic.moduleId()))
                .map(module -> module.learningProgramId().equals(learningProgramId))
                .orElse(false);
    }
}
