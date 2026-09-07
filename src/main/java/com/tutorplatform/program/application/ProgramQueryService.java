package com.tutorplatform.program.application;

import com.tutorplatform.program.domain.ModuleRepository;
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

    public ProgramQueryService(
            StudentProgramRepository studentProgramRepository,
            TopicRepository topicRepository,
            ModuleRepository moduleRepository
    ) {
        this.studentProgramRepository = studentProgramRepository;
        this.topicRepository = topicRepository;
        this.moduleRepository = moduleRepository;
    }

    @Override
    public Optional<StudentProgramContext> findStudentProgram(UUID studentProgramId) {
        return studentProgramRepository.findById(studentProgramId)
                .map(studentProgram -> new StudentProgramContext(
                        studentProgram.getId(),
                        studentProgram.getStudentId(),
                        studentProgram.getLearningProgramId(),
                        studentProgram.getAssignedByTeacherId()
                ));
    }

    @Override
    public boolean topicBelongsToLearningProgram(UUID topicId, UUID learningProgramId) {
        return topicRepository.findById(topicId)
                .flatMap(topic -> moduleRepository.findById(topic.getModuleId()))
                .map(module -> module.getLearningProgramId().equals(learningProgramId))
                .orElse(false);
    }
}
