package com.tutorplatform.submission.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CodeSubmissionDatabaseRepository extends JpaRepository<CodeSubmissionDatabaseModel, UUID> {

    @Query("""
        select code.submissionId, code.executionStatus, code.passedTests, code.totalTests,
               code.executionTimeMs, code.stdoutExcerpt, code.stderrExcerpt
        from CodeSubmissionDatabaseModel code
        where code.submissionId in :submissionIds
        """)
    List<Object[]> findSummaries(@Param("submissionIds") Collection<UUID> submissionIds);
}
