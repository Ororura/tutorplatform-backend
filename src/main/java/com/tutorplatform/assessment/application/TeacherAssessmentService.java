package com.tutorplatform.assessment.application;

import com.tutorplatform.assessment.application.exception.InvalidTeacherAssessmentScoreException;
import com.tutorplatform.assessment.application.exception.TeacherAssessmentConflictException;
import com.tutorplatform.assessment.application.exception.TeacherAssessmentNotFoundException;
import com.tutorplatform.assessment.domain.TeacherAssessmentEntity;
import com.tutorplatform.assessment.domain.TeacherAssessmentRepository;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.session.application.LessonSessionContext;
import com.tutorplatform.session.application.LessonSessionQuery;
import com.tutorplatform.session.application.exception.LessonSessionNotFoundException;
import com.tutorplatform.student.application.ownership.StudentOwnershipQuery;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TeacherAssessmentService {

    private final StudentOwnershipQuery studentOwnershipQuery;
    private final LessonSessionQuery lessonSessionQuery;
    private final TeacherAssessmentRepository assessmentRepository;

    public TeacherAssessmentService(
        StudentOwnershipQuery studentOwnershipQuery,
        LessonSessionQuery lessonSessionQuery,
        TeacherAssessmentRepository assessmentRepository
    ) {
        this.studentOwnershipQuery = studentOwnershipQuery;
        this.lessonSessionQuery = lessonSessionQuery;
        this.assessmentRepository = assessmentRepository;
    }

    @Transactional(readOnly = true)
    public TeacherAssessmentResult getTeacherAssessment(
        AuthenticatedUser principal,
        UUID studentId,
        UUID lessonSessionId
    ) {
        requireOwnedSession(principal, studentId, lessonSessionId, false);
        return assessmentRepository.findByLessonSessionId(lessonSessionId)
            .map(TeacherAssessmentResult::from)
            .orElseThrow(TeacherAssessmentNotFoundException::new);
    }

    @Transactional
    public SaveTeacherAssessmentResult saveTeacherAssessment(
        AuthenticatedUser principal,
        UUID studentId,
        UUID lessonSessionId,
        SaveTeacherAssessmentCommand command
    ) {
        requireOwnedSession(principal, studentId, lessonSessionId, true);
        var existing = assessmentRepository.findByLessonSessionId(lessonSessionId);
        boolean created = existing.isEmpty();
        TeacherAssessmentEntity assessment;
        try {
            assessment = existing.orElseGet(() -> new TeacherAssessmentEntity(
                UUID.randomUUID(), lessonSessionId,
                command.understandingScore(), command.independenceScore(),
                command.practiceScore(), command.homeworkScore(), command.publicComment()
            ));
            if (!created) {
                assessment.update(
                    command.understandingScore(), command.independenceScore(),
                    command.practiceScore(), command.homeworkScore(), command.publicComment()
                );
            }
        } catch (IllegalArgumentException exception) {
            throw new InvalidTeacherAssessmentScoreException(exception);
        }

        try {
            return new SaveTeacherAssessmentResult(
                TeacherAssessmentResult.from(assessmentRepository.saveAndFlush(assessment)),
                created
            );
        } catch (DataIntegrityViolationException exception) {
            throw new TeacherAssessmentConflictException(exception);
        }
    }

    private LessonSessionContext requireOwnedSession(
        AuthenticatedUser principal,
        UUID studentId,
        UUID lessonSessionId,
        boolean forUpdate
    ) {
        UUID teacherId = studentOwnershipQuery.findTeacherIdByUserId(principal.id())
            .orElseThrow(LessonSessionNotFoundException::new);
        LessonSessionContext context = (forUpdate
            ? lessonSessionQuery.findContextByIdForUpdate(lessonSessionId)
            : lessonSessionQuery.findContextById(lessonSessionId))
            .orElseThrow(LessonSessionNotFoundException::new);
        if (!context.teacherId().equals(teacherId)
            || !context.studentId().equals(studentId)
            || !studentOwnershipQuery.isActivePrimaryOwner(teacherId, studentId)) {
            throw new LessonSessionNotFoundException();
        }
        return context;
    }
}
