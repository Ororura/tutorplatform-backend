package com.tutorplatform.submission.api;

import com.tutorplatform.test.PostgresIntegrationTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.homework.domain.HomeworkEntity;
import com.tutorplatform.homework.domain.HomeworkItemEntity;
import com.tutorplatform.homework.domain.HomeworkRepository;
import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentProgramEntity;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import com.tutorplatform.program.domain.studentprogram.StudentProgramStatus;
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkEntity;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkRepository;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import com.tutorplatform.submission.domain.SubmissionAttemptContext;
import com.tutorplatform.submission.domain.SubmissionEntity;
import com.tutorplatform.submission.domain.SubmissionRepository;
import com.tutorplatform.submission.domain.SubmissionStatus;
import com.tutorplatform.task.domain.task.*;
import com.tutorplatform.user.domain.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SubmissionApiIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private TeacherStudentLinkRepository teacherStudentLinkRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private LearningProgramRepository learningProgramRepository;
    @Autowired
    private StudentProgramRepository studentProgramRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private HomeworkRepository homeworkRepository;
    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_submission_api", "008");
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    @Test
    void studentSubmitsTextAnswerWithServerOwnedFieldsAndSequentialAttempts() throws Exception {
        Fixture fixture = createFixture("submit");
        HomeworkEntity homework = createHomework(
            fixture, fixture.studentProgram(), HomeworkStatus.ASSIGNED, List.of(fixture.textTask())
        );
        UUID itemId = homework.getItems().getFirst().id();
        Instant before = Instant.now();

        submit(fixture.studentPrincipal(), fixture.textTask().getId(), itemId,
            request(itemId, "Мой ответ"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(
                "/api/v1/student/submissions/"
            )))
            .andExpect(jsonPath("$.taskId").value(fixture.textTask().getId().toString()))
            .andExpect(jsonPath("$.homeworkItemId").value(itemId.toString()))
            .andExpect(jsonPath("$.attemptNo").value(1))
            .andExpect(jsonPath("$.status").value("NEEDS_REVIEW"))
            .andExpect(jsonPath("$.textAnswer").value("Мой ответ"))
            .andExpect(jsonPath("$.studentId").doesNotExist());

        submit(fixture.studentPrincipal(), fixture.textTask().getId(), itemId,
            request(itemId, "Вторая попытка"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.attemptNo").value(2));

        var attempts = submissionRepository.findAttempts(new SubmissionAttemptContext(
            fixture.student().getId(), fixture.studentProgram().id(),
            fixture.textTask().getId(), itemId
        ), 0, 10);
        assertThat(attempts.items()).hasSize(2).allSatisfy(submission -> {
            assertThat(submission.getStudentId()).isEqualTo(fixture.student().getId());
            assertThat(submission.getStudentProgramId()).isEqualTo(fixture.studentProgram().id());
            assertThat(submission.getTaskId()).isEqualTo(fixture.textTask().getId());
            assertThat(submission.getHomeworkItemId()).isEqualTo(itemId);
            assertThat(submission.getSubmittedAt()).isAfterOrEqualTo(before);
        });
        assertThat(attempts.items()).extracting(SubmissionEntity::getStatus)
            .containsOnly(SubmissionStatus.NEEDS_REVIEW);
        assertThat(homeworkRepository.findById(homework.getId()))
            .get().extracting(HomeworkEntity::getStatus).isEqualTo(HomeworkStatus.ASSIGNED);
    }

    @Test
    void textContractRejectsCodeAndServerOwnedFields() throws Exception {
        Fixture fixture = createFixture("text-contract");
        HomeworkEntity homework = createHomework(
            fixture, fixture.studentProgram(), HomeworkStatus.ASSIGNED, List.of(fixture.textTask())
        );
        UUID itemId = homework.getItems().getFirst().id();

        submit(fixture.studentPrincipal(), fixture.textTask().getId(), itemId,
            """
                {"homeworkItemId":"%s","textAnswer":"Ответ","sourceCode":"pass"}
            """.formatted(itemId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertThat(submissionRepository.findPageByStudentId(fixture.student().getId(), 0, 10).items())
            .isEmpty();
    }

    @Test
    void attemptsAreNumberedSeparatelyByTaskAndHomeworkItemContext() throws Exception {
        Fixture fixture = createFixture("contexts");
        HomeworkEntity firstHomework = createHomework(
            fixture, fixture.studentProgram(), HomeworkStatus.ASSIGNED,
            List.of(fixture.textTask(), fixture.secondTextTask())
        );
        HomeworkEntity secondHomework = createHomework(
            fixture, fixture.studentProgram(), HomeworkStatus.ASSIGNED, List.of(fixture.textTask())
        );
        UUID firstTaskItem = firstHomework.getItems().get(0).id();
        UUID secondTaskItem = firstHomework.getItems().get(1).id();
        UUID otherHomeworkItem = secondHomework.getItems().getFirst().id();

        submit(fixture.studentPrincipal(), fixture.textTask().getId(), firstTaskItem,
            request(firstTaskItem, "A")).andExpect(jsonPath("$.attemptNo").value(1));
        submit(fixture.studentPrincipal(), fixture.secondTextTask().getId(), secondTaskItem,
            request(secondTaskItem, "B")).andExpect(jsonPath("$.attemptNo").value(1));
        submit(fixture.studentPrincipal(), fixture.textTask().getId(), otherHomeworkItem,
            request(otherHomeworkItem, "C")).andExpect(jsonPath("$.attemptNo").value(1));
    }

    @Test
    void concurrentSubmissionsReceiveDistinctAttemptNumbers() throws Exception {
        Fixture fixture = createFixture("concurrent");
        HomeworkEntity homework = createHomework(
            fixture, fixture.studentProgram(), HomeworkStatus.ASSIGNED, List.of(fixture.textTask())
        );
        UUID itemId = homework.getItems().getFirst().id();
        CyclicBarrier start = new CyclicBarrier(2);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> {
                start.await();
                return submit(fixture.studentPrincipal(), fixture.textTask().getId(), itemId,
                    request(itemId, "Параллельный A")).andReturn().getResponse();
            });
            var second = executor.submit(() -> {
                start.await();
                return submit(fixture.studentPrincipal(), fixture.textTask().getId(), itemId,
                    request(itemId, "Параллельный B")).andReturn().getResponse();
            });

            var responses = List.of(first.get(), second.get());
            assertThat(responses).allSatisfy(response -> assertThat(response.getStatus()).isEqualTo(201));
            assertThat(responses).extracting(response ->
                (Integer) com.jayway.jsonpath.JsonPath.read(
                    response.getContentAsString(), "$.attemptNo"
                )
            ).containsExactlyInAnyOrder(1, 2);
        }
    }

    @Test
    void foreignAndMismatchedHomeworkContextsAreRejectedWithoutCreatingSubmission() throws Exception {
        Fixture owner = createFixture("owner-context");
        Fixture foreign = createFixture("foreign-context");
        HomeworkEntity ownerHomework = createHomework(
            owner, owner.studentProgram(), HomeworkStatus.ASSIGNED, List.of(owner.textTask())
        );
        HomeworkEntity foreignHomework = createHomework(
            foreign, foreign.studentProgram(), HomeworkStatus.ASSIGNED, List.of(foreign.textTask())
        );
        UUID ownerItem = ownerHomework.getItems().getFirst().id();
        UUID foreignItem = foreignHomework.getItems().getFirst().id();

        submit(owner.studentPrincipal(), owner.textTask().getId(), foreignItem,
            request(foreignItem, "Чужой контекст"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("HOMEWORK_ITEM_NOT_FOUND"));
        submit(owner.studentPrincipal(), owner.secondTextTask().getId(), ownerItem,
            request(ownerItem, "Не та задача"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("SUBMISSION_CONTEXT_INVALID"));

        assertThat(submissionRepository.findPageByStudentId(owner.student().getId(), 0, 10).items())
            .isEmpty();
    }

    @Test
    void cancelledHomeworkUnknownTaskAndCodeTaskAreRejected() throws Exception {
        Fixture fixture = createFixture("invalid-targets");
        HomeworkEntity cancelled = createHomework(
            fixture, fixture.studentProgram(), HomeworkStatus.CANCELLED, List.of(fixture.textTask())
        );
        UUID cancelledItem = cancelled.getItems().getFirst().id();
        HomeworkEntity codeHomework = createHomework(
            fixture, fixture.studentProgram(), HomeworkStatus.ASSIGNED, List.of(fixture.codeTask())
        );
        UUID codeItem = codeHomework.getItems().getFirst().id();

        submit(fixture.studentPrincipal(), fixture.textTask().getId(), cancelledItem,
            request(cancelledItem, "Ответ"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("HOMEWORK_NOT_SUBMITTABLE"));
        submit(fixture.studentPrincipal(), UUID.randomUUID(), cancelledItem,
            request(cancelledItem, "Ответ"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
        submit(fixture.studentPrincipal(), fixture.codeTask().getId(), codeItem,
            request(codeItem, "print(1)"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("TEXT_SUBMISSION_REQUIRED"));

        assertThat(submissionRepository.findPageByStudentId(fixture.student().getId(), 0, 10).items())
            .isEmpty();
    }

    @Test
    void nullAndBlankAnswersAreRejected() throws Exception {
        Fixture fixture = createFixture("validation");
        HomeworkEntity homework = createHomework(
            fixture, fixture.studentProgram(), HomeworkStatus.ASSIGNED, List.of(fixture.textTask())
        );
        UUID itemId = homework.getItems().getFirst().id();

        submit(fixture.studentPrincipal(), fixture.textTask().getId(), itemId,
            "{\"homeworkItemId\":\"" + itemId + "\",\"textAnswer\":null}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        submit(fixture.studentPrincipal(), fixture.textTask().getId(), itemId,
            request(itemId, "   "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details[0].field").value("textAnswer"));
    }

    @Test
    void listAndSingleGetAreOwnershipScopedAndPaginated() throws Exception {
        Fixture owner = createFixture("read-owner");
        Fixture foreign = createFixture("read-foreign");
        HomeworkEntity ownerHomework = createHomework(
            owner, owner.studentProgram(), HomeworkStatus.ASSIGNED, List.of(owner.textTask())
        );
        HomeworkEntity foreignHomework = createHomework(
            foreign, foreign.studentProgram(), HomeworkStatus.ASSIGNED, List.of(foreign.textTask())
        );
        UUID ownerItem = ownerHomework.getItems().getFirst().id();
        UUID foreignItem = foreignHomework.getItems().getFirst().id();
        for (int i = 0; i < 3; i++) {
            submit(owner.studentPrincipal(), owner.textTask().getId(), ownerItem,
                request(ownerItem, "Owner " + i)).andExpect(status().isCreated());
        }
        String foreignBody = submit(foreign.studentPrincipal(), foreign.textTask().getId(), foreignItem,
            request(foreignItem, "Foreign")).andReturn().getResponse().getContentAsString();
        String foreignId = com.jayway.jsonpath.JsonPath.read(foreignBody, "$.id");

        mockMvc.perform(get("/api/v1/student/tasks/{taskId}/submissions", owner.textTask().getId())
                .with(user(owner.studentPrincipal()))
                .param("homeworkItemId", ownerItem.toString())
                .param("page", "1").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].attemptNo").value(1))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/v1/student/tasks/{taskId}/submissions", owner.textTask().getId())
                .with(user(owner.studentPrincipal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(3));
        mockMvc.perform(get("/api/v1/student/submissions/{submissionId}", foreignId)
                .with(user(owner.studentPrincipal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SUBMISSION_NOT_FOUND"));
    }

    @Test
    void postSecurityRequiresAuthenticationCsrfAndStudentRole() throws Exception {
        UUID taskId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        String request = request(itemId, "Ответ");

        mockMvc.perform(post("/api/v1/student/tasks/{taskId}/submissions", taskId)
                .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        Fixture fixture = createFixture("security");
        mockMvc.perform(post("/api/v1/student/tasks/{taskId}/submissions", taskId)
                .with(user(fixture.studentPrincipal()))
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        AuthenticatedUser teacher = new AuthenticatedUser(
            UUID.randomUUID(), "teacher@example.com", "hash", true,
            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
        mockMvc.perform(post("/api/v1/student/tasks/{taskId}/submissions", taskId)
                .with(user(teacher)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void openApiPublishesSubmissionOperationsAndSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/student/tasks/{taskId}/submissions'].post.operationId")
                .value("submitTextAnswer"))
            .andExpect(jsonPath("$.paths['/api/v1/student/tasks/{taskId}/code-submissions'].post.operationId")
                .value("submitCodeAnswer"))
            .andExpect(jsonPath("$.paths['/api/v1/student/tasks/{taskId}/submissions'].get.operationId")
                .value("listStudentTaskSubmissions"))
            .andExpect(jsonPath("$.paths['/api/v1/student/submissions/{submissionId}'].get.operationId")
                .value("getStudentSubmission"))
            .andExpect(jsonPath("$.components.schemas.StudentSubmissionResponse.properties.id.format")
                .value("uuid"))
            .andExpect(jsonPath("$.components.schemas.StudentSubmissionResponse.properties.submittedAt.format")
                .value("date-time"))
            .andExpect(jsonPath("$.components.schemas.StudentSubmissionResponse.properties.status.enum.length()")
                .value(5))
            .andExpect(jsonPath("$.components.schemas.SubmitTextAnswerRequest.properties.textAnswer").exists())
            .andExpect(jsonPath("$.components.schemas.SubmitTextAnswerRequest.properties.sourceCode").doesNotExist())
            .andExpect(jsonPath("$.components.schemas.SubmitCodeAnswerRequest.properties.sourceCode").exists())
            .andExpect(jsonPath("$.components.schemas.SubmitCodeAnswerRequest.properties.textAnswer").doesNotExist())
            .andExpect(jsonPath("$.components.schemas.ApiError").exists());
    }

    @Test
    void teacherListIsOwnedFilteredPaginatedAndAllowListSorted() throws Exception {
        Fixture fixture = createFixture("teacher-list");
        HomeworkEntity homework = createHomework(
            fixture, fixture.studentProgram(), HomeworkStatus.ASSIGNED, List.of(fixture.textTask())
        );
        UUID itemId = homework.getItems().getFirst().id();
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        saveSubmission(fixture, fixture.textTask(), itemId, 1, SubmissionStatus.NEEDS_REVIEW, "A", base);
        saveSubmission(fixture, fixture.textTask(), itemId, 2, SubmissionStatus.PASSED, "B", base.plusSeconds(1));
        saveSubmission(fixture, fixture.textTask(), itemId, 3, SubmissionStatus.NEEDS_REVIEW, "C", base.plusSeconds(2));

        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        mockMvc.perform(get(teacherSubmissionsUrl(fixture.student().getId()))
                .with(user(fixture.teacherPrincipal()))
                .param("status", "NEEDS_REVIEW")
                .param("page", "0").param("size", "1")
                .param("sort", "attemptNo,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].attemptNo").value(1))
            .andExpect(jsonPath("$.items[0].status").value("NEEDS_REVIEW"))
            .andExpect(jsonPath("$.items[0].studentId").value(fixture.student().getId().toString()))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2));
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(4);

        mockMvc.perform(get(teacherSubmissionsUrl(fixture.student().getId()))
                .with(user(fixture.teacherPrincipal()))
                .param("sort", "submittedAt,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(3))
            .andExpect(jsonPath("$.items[0].attemptNo").value(3))
            .andExpect(jsonPath("$.items[2].attemptNo").value(1));

        mockMvc.perform(get(teacherSubmissionsUrl(fixture.student().getId()))
                .with(user(fixture.teacherPrincipal()))
                .param("sort", "studentId,asc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.details[0].field").value("sort"));
    }

    @Test
    void teacherCannotListOrReadForeignStudentSubmission() throws Exception {
        Fixture owner = createFixture("teacher-owner");
        Fixture foreign = createFixture("teacher-foreign");
        HomeworkEntity foreignHomework = createHomework(
            foreign, foreign.studentProgram(), HomeworkStatus.ASSIGNED, List.of(foreign.textTask())
        );
        SubmissionEntity foreignSubmission = saveSubmission(
            foreign, foreign.textTask(), foreignHomework.getItems().getFirst().id(),
            1, SubmissionStatus.NEEDS_REVIEW, "Foreign", Instant.now()
        );

        mockMvc.perform(get(teacherSubmissionsUrl(foreign.student().getId()))
                .with(user(owner.teacherPrincipal())))
            .andExpect(status().isNotFound());

        review(owner.teacherPrincipal(), owner.student().getId(), foreignSubmission.getId(), "PASSED", true)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SUBMISSION_NOT_FOUND"));

        review(owner.teacherPrincipal(), foreign.student().getId(), foreignSubmission.getId(), "PASSED", true)
            .andExpect(status().isNotFound());
        assertThat(submissionRepository.findById(foreignSubmission.getId()))
            .get().extracting(SubmissionEntity::getStatus)
            .isEqualTo(SubmissionStatus.NEEDS_REVIEW);
    }

    @Test
    void failedReviewDoesNotCompleteHomeworkAndChangesOnlySubmissionStatus() throws Exception {
        Fixture fixture = createFixture("teacher-review");
        HomeworkEntity homework = createHomework(
            fixture, fixture.studentProgram(), HomeworkStatus.ASSIGNED,
            List.of(fixture.textTask(), fixture.secondTextTask())
        );
        UUID firstItemId = homework.getItems().getFirst().id();
        UUID secondItemId = homework.getItems().get(1).id();
        SubmissionEntity passCandidate = saveSubmission(
            fixture, fixture.textTask(), firstItemId, 1, SubmissionStatus.NEEDS_REVIEW,
            "Keep this answer", Instant.parse("2026-02-01T00:00:00Z")
        );
        SubmissionEntity failCandidate = saveSubmission(
            fixture, fixture.secondTextTask(), secondItemId, 1, SubmissionStatus.NEEDS_REVIEW,
            "Keep this too", Instant.parse("2026-02-01T00:01:00Z")
        );
        Integer progressBefore = jdbcTemplate.queryForObject(
            "select count(*) from student_topic_progress", Integer.class
        );

        review(fixture.teacherPrincipal(), fixture.student().getId(), passCandidate.getId(), "PASSED", true)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PASSED"))
            .andExpect(jsonPath("$.textAnswer").value("Keep this answer"));
        review(fixture.teacherPrincipal(), fixture.student().getId(), failCandidate.getId(), "FAILED", true)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("FAILED"));

        SubmissionEntity passed = submissionRepository.findById(passCandidate.getId()).orElseThrow();
        assertThat(passed.getStatus()).isEqualTo(SubmissionStatus.PASSED);
        assertThat(passed.getTextAnswer()).isEqualTo(passCandidate.getTextAnswer());
        assertThat(passed.getAttemptNo()).isEqualTo(passCandidate.getAttemptNo());
        assertThat(passed.getTaskId()).isEqualTo(passCandidate.getTaskId());
        assertThat(passed.getStudentId()).isEqualTo(passCandidate.getStudentId());
        assertThat(passed.getStudentProgramId()).isEqualTo(passCandidate.getStudentProgramId());
        assertThat(passed.getHomeworkItemId()).isEqualTo(passCandidate.getHomeworkItemId());
        assertThat(passed.getSubmittedAt()).isEqualTo(passCandidate.getSubmittedAt());
        HomeworkEntity unchangedHomework = homeworkRepository.findById(homework.getId()).orElseThrow();
        assertThat(unchangedHomework.getStatus()).isEqualTo(HomeworkStatus.ASSIGNED);
        assertThat(unchangedHomework.getCompletedAt()).isNull();
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from student_topic_progress", Integer.class
        )).isEqualTo(progressBefore);
    }

    @Test
    void passingLastRequiredItemAfterReviewCompletesHomework() throws Exception {
        Fixture fixture = createFixture("teacher-review-completion");
        HomeworkEntity homework = createHomework(
            fixture, fixture.studentProgram(), HomeworkStatus.ASSIGNED,
            List.of(fixture.textTask(), fixture.secondTextTask())
        );
        HomeworkItemEntity firstItem = homework.getItems().getFirst();
        HomeworkItemEntity lastItem = homework.getItems().get(1);
        saveSubmission(
            fixture, fixture.textTask(), firstItem.id(), 1, SubmissionStatus.PASSED,
            "Already passed", Instant.parse("2026-02-01T00:00:00Z")
        );
        SubmissionEntity lastSubmission = saveSubmission(
            fixture, fixture.secondTextTask(), lastItem.id(), 1, SubmissionStatus.NEEDS_REVIEW,
            "Last answer", Instant.parse("2026-02-01T00:01:00Z")
        );

        review(
            fixture.teacherPrincipal(), fixture.student().getId(), lastSubmission.getId(),
            "PASSED", true
        ).andExpect(status().isOk());

        HomeworkEntity completed = homeworkRepository.findById(homework.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(HomeworkStatus.COMPLETED);
        assertThat(completed.getCompletedAt()).isNotNull();
    }

    @Test
    void finalTechnicalAndCodeSubmissionsCannotBeReviewed() throws Exception {
        Fixture fixture = createFixture("teacher-review-state");
        HomeworkEntity homework = createHomework(
            fixture, fixture.studentProgram(), HomeworkStatus.ASSIGNED,
            List.of(fixture.textTask(), fixture.codeTask())
        );
        UUID textItem = homework.getItems().get(0).id();
        UUID codeItem = homework.getItems().get(1).id();
        SubmissionEntity passed = saveSubmission(
            fixture, fixture.textTask(), textItem, 1, SubmissionStatus.PASSED, "passed", Instant.now()
        );
        SubmissionEntity failed = saveSubmission(
            fixture, fixture.textTask(), textItem, 2, SubmissionStatus.FAILED, "failed", Instant.now()
        );
        SubmissionEntity systemError = saveSubmission(
            fixture, fixture.textTask(), textItem, 3, SubmissionStatus.SYSTEM_ERROR, "error", Instant.now()
        );
        SubmissionEntity code = saveSubmission(
            fixture, fixture.codeTask(), codeItem, 1, SubmissionStatus.NEEDS_REVIEW, null, Instant.now()
        );

        for (SubmissionEntity submission : List.of(passed, failed, systemError, code)) {
            review(fixture.teacherPrincipal(), fixture.student().getId(), submission.getId(), "PASSED", true)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUBMISSION_NOT_REVIEWABLE"));
            assertThat(submissionRepository.findById(submission.getId()))
                .get().extracting(SubmissionEntity::getStatus).isEqualTo(submission.getStatus());
        }

        SubmissionEntity pending = saveSubmission(
            fixture, fixture.textTask(), textItem, 4, SubmissionStatus.NEEDS_REVIEW, "pending", Instant.now()
        );
        review(fixture.teacherPrincipal(), fixture.student().getId(), pending.getId(), "NEEDS_REVIEW", true)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_SUBMISSION_REVIEW_STATUS"));
        review(fixture.teacherPrincipal(), fixture.student().getId(), pending.getId(), "UNKNOWN", true)
            .andExpect(status().isBadRequest());
    }

    @Test
    void teacherReviewSecurityRequiresAuthenticationTeacherRoleAndCsrf() throws Exception {
        Fixture fixture = createFixture("teacher-review-security");
        UUID submissionId = UUID.randomUUID();

        review(null, fixture.student().getId(), submissionId, "PASSED", true)
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
        review(fixture.studentPrincipal(), fixture.student().getId(), submissionId, "PASSED", true)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        review(fixture.teacherPrincipal(), fixture.student().getId(), submissionId, "PASSED", false)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void openApiPublishesTeacherSubmissionOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/submissions'].get.operationId")
                .value("listTeacherStudentSubmissions"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/submissions/{submissionId}/review'].patch.operationId")
                .value("reviewTextSubmission"))
            .andExpect(jsonPath("$.components.schemas.TeacherSubmissionResponse.properties.id.format")
                .value("uuid"))
            .andExpect(jsonPath("$.components.schemas.TeacherSubmissionResponse.properties.status.enum.length()")
                .value(5))
            .andExpect(jsonPath("$.components.schemas.ApiError").exists());
    }

    private ResultActions submit(
        AuthenticatedUser principal,
        UUID taskId,
        UUID homeworkItemId,
        String body
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/student/tasks/{taskId}/submissions", taskId)
            .with(user(principal)).with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
    }

    private ResultActions review(
        AuthenticatedUser principal,
        UUID studentId,
        UUID submissionId,
        String statusValue,
        boolean includeCsrf
    ) throws Exception {
        var request = patch(
            "/api/v1/teacher/students/{studentId}/submissions/{submissionId}/review",
            studentId, submissionId
        ).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + statusValue + "\"}");
        if (principal != null) {
            request.with(user(principal));
        }
        if (includeCsrf) {
            request.with(csrf());
        }
        return mockMvc.perform(request);
    }

    private String teacherSubmissionsUrl(UUID studentId) {
        return "/api/v1/teacher/students/" + studentId + "/submissions";
    }

    private SubmissionEntity saveSubmission(
        Fixture fixture,
        TaskEntity task,
        UUID homeworkItemId,
        int attemptNo,
        SubmissionStatus status,
        String textAnswer,
        Instant submittedAt
    ) {
        return submissionRepository.saveAndFlush(new SubmissionEntity(
            UUID.randomUUID(), fixture.student().getId(), fixture.studentProgram().id(),
            task.getId(), homeworkItemId, attemptNo, status, textAnswer, submittedAt
        ));
    }

    private String request(UUID homeworkItemId, String textAnswer) {
        return """
            {"homeworkItemId":"%s","textAnswer":"%s"}
            """.formatted(homeworkItemId, textAnswer);
    }

    private Fixture createFixture(String label) {
        UserEntity teacherUser = new UserEntity(
            UUID.randomUUID(), label + "-teacher-" + UUID.randomUUID() + "@example.com",
            "hash", UserStatus.ACTIVE
        );
        teacherUser.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(teacherUser);
        TeacherEntity teacher = teacherRepository.saveAndFlush(new TeacherEntity(
            UUID.randomUUID(), teacherUser, "Teacher"
        ));
        UserEntity studentUser = new UserEntity(
            UUID.randomUUID(), label + "-student-" + UUID.randomUUID() + "@example.com",
            "hash", UserStatus.ACTIVE
        );
        studentUser.addRole(UserRole.STUDENT);
        userRepository.saveAndFlush(studentUser);
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(
            UUID.randomUUID(), studentUser.id(), "Student", null,
            StudentStatus.ACTIVE, null, null
        ));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));
        SubjectEntity subject = subjectRepository.saveAndFlush(new SubjectEntity(
            UUID.randomUUID(), teacher.id(), null, "Subject " + UUID.randomUUID(), null,
            SubjectStatus.ACTIVE
        ));
        LearningProgramEntity program = learningProgramRepository.saveAndFlush(new LearningProgramEntity(
            UUID.randomUUID(), teacher.id(), subject.id(), "Program", null,
            LearningProgramStatus.ACTIVE
        ));
        StudentProgramEntity studentProgram = studentProgramRepository.saveAndFlush(new StudentProgramEntity(
            UUID.randomUUID(), student.getId(), program.getId(), teacher.id(),
            StudentProgramStatus.ACTIVE, 480, Instant.now(), null
        ));
        AuthenticatedUser studentPrincipal = new AuthenticatedUser(
            studentUser.id(), studentUser.email(), "hash", true,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        AuthenticatedUser teacherPrincipal = new AuthenticatedUser(
            teacherUser.id(), teacherUser.email(), "hash", true,
            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
        TaskEntity textTask = createTask(teacher, subject, TaskType.TEXT, "Text task");
        TaskEntity secondTextTask = createTask(teacher, subject, TaskType.TEXT, "Second text task");
        TaskEntity codeTask = createTask(teacher, subject, TaskType.CODE, "Code task");
        return new Fixture(teacher, student, studentPrincipal, teacherPrincipal, subject, studentProgram,
            textTask, secondTextTask, codeTask);
    }

    private TaskEntity createTask(
        TeacherEntity teacher,
        SubjectEntity subject,
        TaskType type,
        String title
    ) {
        return taskRepository.saveAndFlush(new TaskEntity(
            UUID.randomUUID(), teacher.id(), subject.id(), title, "Description",
            type, TaskDifficulty.MEDIUM, TaskStatus.ACTIVE
        ));
    }

    private HomeworkEntity createHomework(
        Fixture fixture,
        StudentProgramEntity studentProgram,
        HomeworkStatus status,
        List<TaskEntity> tasks
    ) {
        UUID homeworkId = UUID.randomUUID();
        List<HomeworkItemEntity> items = java.util.stream.IntStream.range(0, tasks.size())
            .mapToObj(index -> new HomeworkItemEntity(
                UUID.randomUUID(), homeworkId, tasks.get(index).getId(), index, true
            ))
            .toList();
        return homeworkRepository.saveAndFlush(new HomeworkEntity(
            homeworkId, studentProgram.id(), fixture.teacher().id(), "Homework", null,
            Instant.now(), null, status, null, items
        ));
    }

    private record Fixture(
        TeacherEntity teacher,
        StudentEntity student,
        AuthenticatedUser studentPrincipal,
        AuthenticatedUser teacherPrincipal,
        SubjectEntity subject,
        StudentProgramEntity studentProgram,
        TaskEntity textTask,
        TaskEntity secondTextTask,
        TaskEntity codeTask
    ) {
    }
}
