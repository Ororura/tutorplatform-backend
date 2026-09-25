package com.tutorplatform.report.infrastructure.persistence;

import com.tutorplatform.report.application.ProgressReportReadQuery;
import com.tutorplatform.report.application.ProgressReportSummary;
import com.tutorplatform.report.application.ProgressReportSummaryPage;
import com.tutorplatform.report.domain.ProgressReportStatus;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProgressReportReadQuery implements ProgressReportReadQuery {

    private static final Map<String, String> SORT_COLUMNS =
            Map.of(
                    "createdAt", "report.created_at",
                    "updatedAt", "report.updated_at",
                    "periodStartedAt", "report.period_started_at",
                    "periodEndedAt", "report.period_ended_at",
                    "publishedAt", "report.published_at",
                    "status", "report.status");

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcProgressReportReadQuery(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<UUID, UUID> findIdsByLearningPeriodIds(Collection<UUID> learningPeriodIds) {
        if (learningPeriodIds.isEmpty()) {
            return Map.of();
        }
        return jdbc
                .query(
                        """
                select learning_period_id, id
                from progress_reports
                where learning_period_id in (:learningPeriodIds)
                """,
                        new MapSqlParameterSource("learningPeriodIds", learningPeriodIds),
                        (row, rowNumber) ->
                                Map.entry(
                                        row.getObject("learning_period_id", UUID.class),
                                        row.getObject("id", UUID.class)))
                .stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public ProgressReportSummaryPage findOwnedPage(
            UUID teacherId,
            UUID studentProgramId,
            ProgressReportStatus status,
            int page,
            int size,
            String sortField,
            boolean ascending) {
        String sortColumn = SORT_COLUMNS.get(sortField);
        if (sortColumn == null) {
            throw new IllegalArgumentException("Unsupported ProgressReport sort field");
        }
        var parameters =
                new MapSqlParameterSource()
                        .addValue("teacherId", teacherId)
                        .addValue("limit", size)
                        .addValue("offset", Math.multiplyExact((long) page, size));
        StringBuilder fromAndFilters =
                new StringBuilder(
                        """
            from progress_reports report
            join student_programs program on program.id = report.student_program_id
            join teacher_student_links ownership
              on ownership.teacher_id = :teacherId
             and ownership.student_id = program.student_id
             and ownership.relation_type = 'PRIMARY'
             and ownership.ended_at is null
            where program.assigned_by_teacher_id = :teacherId
            """);
        if (studentProgramId != null) {
            fromAndFilters.append(" and report.student_program_id = :studentProgramId");
            parameters.addValue("studentProgramId", studentProgramId);
        }
        if (status != null) {
            fromAndFilters.append(" and report.status = :status");
            parameters.addValue("status", status.name());
        }
        String direction = ascending ? " asc" : " desc";
        List<ProgressReportSummary> items =
                jdbc.query(
                        """
                select report.id, report.student_program_id, report.learning_period_id,
                       report.status, report.period_started_at, report.period_ended_at,
                       report.learning_minutes, report.published_at,
                       report.created_at, report.updated_at
                """
                                + fromAndFilters
                                + " order by "
                                + sortColumn
                                + direction
                                + ", report.id desc limit :limit offset :offset",
                        parameters,
                        (row, rowNumber) ->
                                new ProgressReportSummary(
                                        row.getObject("id", UUID.class),
                                        row.getObject("student_program_id", UUID.class),
                                        row.getObject("learning_period_id", UUID.class),
                                        ProgressReportStatus.valueOf(row.getString("status")),
                                        row.getTimestamp("period_started_at").toInstant(),
                                        row.getTimestamp("period_ended_at").toInstant(),
                                        row.getInt("learning_minutes"),
                                        row.getTimestamp("published_at") == null
                                                ? null
                                                : row.getTimestamp("published_at").toInstant(),
                                        row.getTimestamp("created_at").toInstant(),
                                        row.getTimestamp("updated_at").toInstant()));
        long totalElements =
                jdbc.queryForObject("select count(*) " + fromAndFilters, parameters, Long.class);
        int totalPages =
                totalElements == 0 ? 0 : Math.toIntExact((totalElements + size - 1) / size);
        return new ProgressReportSummaryPage(items, totalElements, totalPages);
    }
}
