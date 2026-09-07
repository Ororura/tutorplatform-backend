package com.tutorplatform.program.infrastructure.persistence.studentprogram;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class StudentTopicProgressId implements Serializable {

    @Column(name = "student_program_id", nullable = false)
    private UUID studentProgramId;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    protected StudentTopicProgressId() {
    }

    public StudentTopicProgressId(UUID studentProgramId, UUID topicId) {
        this.studentProgramId = Objects.requireNonNull(studentProgramId);
        this.topicId = Objects.requireNonNull(topicId);
    }

    public UUID getStudentProgramId() { return studentProgramId; }
    public UUID getTopicId() { return topicId; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof StudentTopicProgressId that)) return false;
        return studentProgramId.equals(that.studentProgramId) && topicId.equals(that.topicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(studentProgramId, topicId);
    }
}
