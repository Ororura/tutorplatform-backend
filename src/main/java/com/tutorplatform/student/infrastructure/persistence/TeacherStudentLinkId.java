package com.tutorplatform.student.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class TeacherStudentLinkId implements Serializable {

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    protected TeacherStudentLinkId() {
    }

    public TeacherStudentLinkId(UUID teacherId, UUID studentId) {
        this.teacherId = Objects.requireNonNull(teacherId);
        this.studentId = Objects.requireNonNull(studentId);
    }

    public UUID getTeacherId() {
        return teacherId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TeacherStudentLinkId that)) {
            return false;
        }
        return teacherId.equals(that.teacherId) && studentId.equals(that.studentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teacherId, studentId);
    }
}
