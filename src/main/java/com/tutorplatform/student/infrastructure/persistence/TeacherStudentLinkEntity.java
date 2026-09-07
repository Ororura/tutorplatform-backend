package com.tutorplatform.student.infrastructure.persistence;

import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.TeacherStudentRelationType;
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.infrastructure.persistence.TeacherDatabaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "teacher_student_links")
public class TeacherStudentLinkEntity {

    @EmbeddedId
    private TeacherStudentLinkId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false, insertable = false, updatable = false)
    private TeacherDatabaseModel teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, insertable = false, updatable = false)
    private StudentDatabaseModel student;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 24)
    private TeacherStudentRelationType relationType;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TeacherStudentLinkEntity() {
    }

    public TeacherStudentLinkEntity(TeacherEntity teacher, StudentEntity student) {
        Objects.requireNonNull(teacher);
        Objects.requireNonNull(student);
        this.id = new TeacherStudentLinkId(teacher.getId(), student.getId());
        this.relationType = TeacherStudentRelationType.PRIMARY;
    }

    public TeacherStudentLinkId getId() {
        return id;
    }

    public TeacherDatabaseModel getTeacher() {
        return teacher;
    }

    public StudentDatabaseModel getStudent() {
        return student;
    }

    public TeacherStudentRelationType getRelationType() {
        return relationType;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
