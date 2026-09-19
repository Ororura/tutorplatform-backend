package com.tutorplatform.program.api;

import com.tutorplatform.test.PostgresIntegrationTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.program.domain.ModuleEntity;
import com.tutorplatform.program.domain.ModuleRepository;
import com.tutorplatform.program.domain.TopicEntity;
import com.tutorplatform.program.domain.TopicRepository;
import com.tutorplatform.program.domain.TopicStatus;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import com.tutorplatform.program.domain.studentprogram.StudentProgramEntity;
import com.tutorplatform.program.domain.studentprogram.StudentProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressRepository;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressStatus;
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkEntity;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkRepository;
import com.tutorplatform.subject.domain.SubjectEntity;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.subject.domain.SubjectStatus;
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TeacherProgramManagementApiIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_teacher_program_management_api", null);
    }
    private static final UUID PYTHON = UUID.fromString("6513554d-dceb-5902-b6df-557c7a94b5c7");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired TeacherRepository teacherRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired TeacherStudentLinkRepository linkRepository;
    @Autowired SubjectRepository subjectRepository;
    @Autowired LearningProgramRepository learningProgramRepository;
    @Autowired StudentProgramRepository studentProgramRepository;
    @Autowired ModuleRepository moduleRepository;
    @Autowired TopicRepository topicRepository;
    @Autowired StudentTopicProgressRepository progressRepository;

    @Test
    void subjectsReturnSystemAndOwnButNotForeignAndDefaultToActive() throws Exception {
        TeacherContext teacher = teacher("subjects-owner@example.com");
        TeacherContext foreign = teacher("subjects-foreign@example.com");
        SubjectEntity own = subject(teacher.teacher.id(), "Own", SubjectStatus.ACTIVE);
        subject(foreign.teacher.id(), "Foreign", SubjectStatus.ACTIVE);
        SubjectEntity archived = subject(teacher.teacher.id(), "Archived", SubjectStatus.ARCHIVED);

        mockMvc.perform(get("/api/v1/teacher/subjects").with(user(teacher.principal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id == '%s')]", PYTHON).exists())
            .andExpect(jsonPath("$[?(@.id == '%s')]", own.id()).exists())
            .andExpect(jsonPath("$[?(@.name == 'Foreign')]").isEmpty())
            .andExpect(jsonPath("$[?(@.id == '%s')]", archived.id()).isEmpty());

        mockMvc.perform(get("/api/v1/teacher/subjects?status=ARCHIVED").with(user(teacher.principal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(archived.id().toString()));
    }

    @Test
    void archivesDraftActiveAndArchivedProgramsAndIsIdempotent() throws Exception {
        TeacherContext teacher = teacher("program-archive-transitions@example.com");
        LearningProgramEntity draft = program(teacher.teacher, "Draft", LearningProgramStatus.DRAFT);
        LearningProgramEntity active = program(teacher.teacher, "Active", LearningProgramStatus.ACTIVE);
        LearningProgramEntity archived = program(teacher.teacher, "Archived", LearningProgramStatus.ARCHIVED);

        archive(teacher, draft);
        archive(teacher, active);
        archive(teacher, archived);
        archive(teacher, archived);

        assertThat(learningProgramRepository.findById(draft.getId()).orElseThrow().getStatus())
            .isEqualTo(LearningProgramStatus.ARCHIVED);
        assertThat(learningProgramRepository.findById(active.getId()).orElseThrow().getStatus())
            .isEqualTo(LearningProgramStatus.ARCHIVED);
        assertThat(learningProgramRepository.findById(archived.getId()).orElseThrow().getStatus())
            .isEqualTo(LearningProgramStatus.ARCHIVED);
    }

    @Test
    void archiveHidesForeignProgramAndPreservesExistingAssignment() throws Exception {
        TeacherContext teacher = teacher("program-archive-owner@example.com");
        TeacherContext foreign = teacher("program-archive-foreign@example.com");
        LearningProgramEntity program = program(teacher.teacher, "Assigned", LearningProgramStatus.ACTIVE);
        LearningProgramEntity foreignProgram = program(foreign.teacher, "Foreign", LearningProgramStatus.ACTIVE);
        StudentEntity student = student(teacher.teacher, "Assigned student");
        StudentProgramEntity assignment = studentProgramRepository.saveAndFlush(new StudentProgramEntity(
            UUID.randomUUID(), student.getId(), program.getId(), teacher.teacher.id(),
            StudentProgramStatus.ACTIVE, 480, Instant.parse("2026-09-01T00:00:00Z"), null
        ));

        mockMvc.perform(post("/api/v1/teacher/programs/{programId}/archive", foreignProgram.getId())
                .with(user(teacher.principal)).with(csrf()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_NOT_FOUND"));

        archive(teacher, program);

        StudentProgramEntity persisted = studentProgramRepository.findById(assignment.id()).orElseThrow();
        assertThat(persisted.studentId()).isEqualTo(student.getId());
        assertThat(persisted.learningProgramId()).isEqualTo(program.getId());
        assertThat(persisted.status()).isEqualTo(StudentProgramStatus.ACTIVE);
        expectAssign(teacher, student.getId(), program.getId(), null, 409, "LEARNING_PROGRAM_STATUS_CONFLICT");
    }

    private void archive(TeacherContext teacher, LearningProgramEntity program) throws Exception {
        mockMvc.perform(post("/api/v1/teacher/programs/{programId}/archive", program.getId())
                .with(user(teacher.principal)).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(program.getId().toString()))
            .andExpect(jsonPath("$.status").value("ARCHIVED"))
            .andExpect(jsonPath("$.subject.id").value(PYTHON.toString()));
    }

    @Test
    void subjectEndpointRequiresTeacher() throws Exception {
        mockMvc.perform(get("/api/v1/teacher/subjects")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/teacher/subjects").with(user(studentPrincipal())))
            .andExpect(status().isForbidden());
    }

    @Test
    void createListAndActivateOwnTemplateWithAuthorizedSubjects() throws Exception {
        TeacherContext teacher = teacher("program-create@example.com");
        TeacherContext foreign = teacher("program-create-foreign@example.com");
        SubjectEntity own = subject(teacher.teacher.id(), "Teacher subject", SubjectStatus.ACTIVE);
        SubjectEntity foreignSubject = subject(foreign.teacher.id(), "Foreign subject", SubjectStatus.ACTIVE);
        program(foreign.teacher, "Foreign template", LearningProgramStatus.DRAFT);

        String body = objectMapper.writeValueAsString(new CreateLearningProgramRequest(
            own.id(), "  Python с нуля  ", "Описание"
        ));
        String response = mockMvc.perform(post("/api/v1/teacher/programs")
                .with(user(teacher.principal)).with(csrf()).contentType("application/json").content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Python с нуля"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.subject.id").value(own.id().toString()))
            .andReturn().getResponse().getContentAsString();
        UUID programId = UUID.fromString(objectMapper.readTree(response).get("id").textValue());
        assertThat(learningProgramRepository.findById(programId).orElseThrow().getTeacherId())
            .isEqualTo(teacher.teacher.id());

        mockMvc.perform(get("/api/v1/teacher/programs?status=DRAFT").with(user(teacher.principal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(programId.toString()))
            .andExpect(jsonPath("$[0].modules").doesNotExist())
            .andExpect(jsonPath("$[?(@.title == 'Foreign template')]").isEmpty());

        mockMvc.perform(post("/api/v1/teacher/programs").with(user(teacher.principal)).with(csrf())
                .contentType("application/json").content(objectMapper.writeValueAsString(
                    new CreateLearningProgramRequest(PYTHON, "System Python", null))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.subject.code").value("PYTHON"));

        mockMvc.perform(post("/api/v1/teacher/programs/{id}/activate", programId)
                .with(user(teacher.principal)).with(csrf()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/teacher/programs").with(user(teacher.principal)).with(csrf())
                .contentType("application/json").content(objectMapper.writeValueAsString(
                    new CreateLearningProgramRequest(foreignSubject.id(), "Denied", null))))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SUBJECT_NOT_FOUND"));
    }

    @Test
    void getsOwnedProgramDetailsWithOrderedStructureAndAssignmentFlags() throws Exception {
        TeacherContext teacher = teacher("program-details@example.com");
        LearningProgramEntity program = program(teacher.teacher, "Details", LearningProgramStatus.ACTIVE);
        ModuleEntity laterModule = moduleRepository.saveAndFlush(new ModuleEntity(
            UUID.randomUUID(), program.getId(), "Later", "Later description", 2
        ));
        ModuleEntity firstModule = moduleRepository.saveAndFlush(new ModuleEntity(
            UUID.randomUUID(), program.getId(), "First", "First description", 1
        ));
        TopicEntity laterTopic = topicRepository.saveAndFlush(new TopicEntity(
            UUID.randomUUID(), firstModule.id(), "Later topic", "Later topic description", 3, TopicStatus.DRAFT
        ));
        TopicEntity firstTopic = topicRepository.saveAndFlush(new TopicEntity(
            UUID.randomUUID(), firstModule.id(), "First topic", "First topic description", 1, TopicStatus.ACTIVE
        ));
        StudentEntity student = student(teacher.teacher, "Assigned");
        studentProgramRepository.saveAndFlush(new StudentProgramEntity(
            UUID.randomUUID(), student.getId(), program.getId(), teacher.teacher.id(),
            StudentProgramStatus.ACTIVE, 480, Instant.parse("2026-09-01T00:00:00Z"), null
        ));

        mockMvc.perform(get("/api/v1/teacher/programs/{programId}", program.getId()).with(user(teacher.principal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(program.getId().toString()))
            .andExpect(jsonPath("$.subject.id").value(PYTHON.toString()))
            .andExpect(jsonPath("$.title").value("Details"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.version").isNumber())
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())
            .andExpect(jsonPath("$.hasAssignments").value(true))
            .andExpect(jsonPath("$.editable").value(false))
            .andExpect(jsonPath("$.modules[0].id").value(firstModule.id().toString()))
            .andExpect(jsonPath("$.modules[0].position").value(1))
            .andExpect(jsonPath("$.modules[1].id").value(laterModule.id().toString()))
            .andExpect(jsonPath("$.modules[0].topics[0].id").value(firstTopic.id().toString()))
            .andExpect(jsonPath("$.modules[0].topics[0].status").value("ACTIVE"))
            .andExpect(jsonPath("$.modules[0].topics[0].version").isNumber())
            .andExpect(jsonPath("$.modules[0].topics[0].progressStatus").doesNotExist())
            .andExpect(jsonPath("$.modules[0].topics[1].id").value(laterTopic.id().toString()));
    }

    @Test
    void doesNotExposeForeignOrMissingProgramDetails() throws Exception {
        TeacherContext teacher = teacher("program-details-owner@example.com");
        TeacherContext foreign = teacher("program-details-foreign@example.com");
        LearningProgramEntity foreignProgram = program(foreign.teacher, "Foreign", LearningProgramStatus.DRAFT);

        mockMvc.perform(get("/api/v1/teacher/programs/{programId}", foreignProgram.getId()).with(user(teacher.principal)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/teacher/programs/{programId}", UUID.randomUUID()).with(user(teacher.principal)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_NOT_FOUND"));
    }

    @Test
    void detailsAreEditableOnlyForUnassignedNonArchivedProgram() throws Exception {
        TeacherContext teacher = teacher("program-details-editable@example.com");
        LearningProgramEntity draft = program(teacher.teacher, "Draft", LearningProgramStatus.DRAFT);
        LearningProgramEntity archived = program(teacher.teacher, "Archived", LearningProgramStatus.ARCHIVED);

        mockMvc.perform(get("/api/v1/teacher/programs/{programId}", draft.getId()).with(user(teacher.principal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasAssignments").value(false))
            .andExpect(jsonPath("$.editable").value(true));
        mockMvc.perform(get("/api/v1/teacher/programs/{programId}", archived.getId()).with(user(teacher.principal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hasAssignments").value(false))
            .andExpect(jsonPath("$.editable").value(false));
    }

    @Test
    void patchUpdatesOwnedDraftAndActiveProgramsWithoutChangingSubjectOrStatus() throws Exception {
        TeacherContext teacher = teacher("program-update@example.com");
        LearningProgramEntity draft = program(teacher.teacher, "Draft", LearningProgramStatus.DRAFT);
        LearningProgramEntity active = program(teacher.teacher, "Active", LearningProgramStatus.ACTIVE);

        mockMvc.perform(patch("/api/v1/teacher/programs/{programId}", draft.getId())
                .with(user(teacher.principal)).with(csrf()).contentType("application/json")
                .content(updateRequest("  Python с нуля  ", "Описание", draft.getVersion())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Python с нуля"))
            .andExpect(jsonPath("$.description").value("Описание"))
            .andExpect(jsonPath("$.subject.id").value(PYTHON.toString()))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.version").value(draft.getVersion() + 1));

        mockMvc.perform(patch("/api/v1/teacher/programs/{programId}", active.getId())
                .with(user(teacher.principal)).with(csrf()).contentType("application/json")
                .content(updateRequest("Updated active", null, active.getVersion())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated active"))
            .andExpect(jsonPath("$.description").doesNotExist())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void patchHidesForeignProgram() throws Exception {
        TeacherContext owner = teacher("program-update-owner@example.com");
        TeacherContext foreign = teacher("program-update-foreign@example.com");
        LearningProgramEntity program = program(owner.teacher, "Owned", LearningProgramStatus.DRAFT);

        mockMvc.perform(patch("/api/v1/teacher/programs/{programId}", program.getId())
                .with(user(foreign.principal)).with(csrf()).contentType("application/json")
                .content(updateRequest("Denied", null, program.getVersion())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_NOT_FOUND"));
    }

    @Test
    void patchValidatesTrimmedTitleAndVersion() throws Exception {
        TeacherContext teacher = teacher("program-update-validation@example.com");
        LearningProgramEntity program = program(teacher.teacher, "Valid", LearningProgramStatus.DRAFT);
        String tooLong = "x".repeat(201);

        for (String body : List.of(
            updateRequest("   ", null, program.getVersion()),
            updateRequest(tooLong, null, program.getVersion()),
            "{\"title\":\"Valid\",\"description\":null}",
            "{\"title\":\"Valid\",\"description\":null,\"version\":-1}"
        )) {
            mockMvc.perform(patch("/api/v1/teacher/programs/{programId}", program.getId())
                    .with(user(teacher.principal)).with(csrf()).contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void stalePatchReturnsVersionConflict() throws Exception {
        TeacherContext teacher = teacher("program-update-version@example.com");
        LearningProgramEntity program = program(teacher.teacher, "Initial", LearningProgramStatus.DRAFT);
        String firstVersionUpdate = updateRequest("First", null, program.getVersion());

        mockMvc.perform(patch("/api/v1/teacher/programs/{programId}", program.getId())
                .with(user(teacher.principal)).with(csrf()).contentType("application/json")
                .content(firstVersionUpdate))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/teacher/programs/{programId}", program.getId())
                .with(user(teacher.principal)).with(csrf()).contentType("application/json")
                .content(firstVersionUpdate))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_VERSION_CONFLICT"));
    }

    @Test
    void patchRejectsArchivedAndEverAssignedPrograms() throws Exception {
        TeacherContext teacher = teacher("program-update-state@example.com");
        LearningProgramEntity archived = program(teacher.teacher, "Archived", LearningProgramStatus.ARCHIVED);
        LearningProgramEntity assigned = program(teacher.teacher, "Assigned", LearningProgramStatus.ACTIVE);
        StudentEntity student = student(teacher.teacher, "Completed student");
        studentProgramRepository.saveAndFlush(new StudentProgramEntity(
            UUID.randomUUID(), student.getId(), assigned.getId(), teacher.teacher.id(),
            StudentProgramStatus.COMPLETED, 480, Instant.parse("2026-09-01T00:00:00Z"),
            Instant.parse("2026-09-02T00:00:00Z")
        ));

        expectUpdateConflict(teacher, archived, "LEARNING_PROGRAM_STATUS_CONFLICT");
        expectUpdateConflict(teacher, assigned, "LEARNING_PROGRAM_STATUS_CONFLICT");
    }

    @Test
    void assignmentIsOwnedActiveAtomicVisibleAndInitializesLockedProgress() throws Exception {
        TeacherContext teacher = teacher("assign@example.com");
        StudentEntity student = student(teacher.teacher, "Илья");
        LearningProgramEntity program = program(teacher.teacher, "Python template", LearningProgramStatus.ACTIVE);
        ModuleEntity module = moduleRepository.saveAndFlush(new ModuleEntity(UUID.randomUUID(), program.getId(), "M", null, 0));
        TopicEntity topic = topicRepository.saveAndFlush(new TopicEntity(UUID.randomUUID(), module.id(), "T", null, 0, TopicStatus.ACTIVE));

        String body = "{\"learningProgramId\":\"" + program.getId() + "\"}";
        String response = mockMvc.perform(post("/api/v1/teacher/students/{id}/programs", student.getId())
                .with(user(teacher.principal)).with(csrf()).contentType("application/json").content(body))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.reportIntervalMinutes").value(480))
            .andExpect(jsonPath("$.title").value("Python template"))
            .andReturn().getResponse().getContentAsString();
        UUID studentProgramId = UUID.fromString(objectMapper.readTree(response).get("id").textValue());

        var assigned = studentProgramRepository.findById(studentProgramId).orElseThrow();
        assertThat(assigned.studentId()).isEqualTo(student.getId());
        assertThat(assigned.assignedByTeacherId()).isEqualTo(teacher.teacher.id());
        assertThat(progressRepository.findById(studentProgramId, topic.id()).orElseThrow().status())
            .isEqualTo(StudentTopicProgressStatus.LOCKED);

        mockMvc.perform(get("/api/v1/teacher/students/{id}/programs", student.getId()).with(user(teacher.principal)))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(studentProgramId.toString()));
        mockMvc.perform(get("/api/v1/teacher/students/{studentId}/programs/{programId}", student.getId(), studentProgramId)
                .with(user(teacher.principal)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.modules[0].topics[0].progressStatus").value("LOCKED"));

        mockMvc.perform(post("/api/v1/teacher/students/{id}/programs", student.getId())
                .with(user(teacher.principal)).with(csrf()).contentType("application/json").content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("STUDENT_PROGRAM_ALREADY_ASSIGNED"));
    }

    @Test
    void assignmentRejectsForeignResourcesDraftArchivedAndInvalidIntervalButAllowsDifferentProgram() throws Exception {
        TeacherContext teacher = teacher("assign-owner@example.com");
        TeacherContext foreign = teacher("assign-foreign@example.com");
        StudentEntity ownStudent = student(teacher.teacher, "Own");
        StudentEntity foreignStudent = student(foreign.teacher, "Foreign");
        LearningProgramEntity active = program(teacher.teacher, "Active", LearningProgramStatus.ACTIVE);
        LearningProgramEntity second = program(teacher.teacher, "Second", LearningProgramStatus.ACTIVE);
        LearningProgramEntity draft = program(teacher.teacher, "Draft", LearningProgramStatus.DRAFT);
        LearningProgramEntity archived = program(teacher.teacher, "Archived", LearningProgramStatus.ARCHIVED);
        LearningProgramEntity foreignProgram = program(foreign.teacher, "Foreign", LearningProgramStatus.ACTIVE);

        expectAssign(teacher, foreignStudent.getId(), active.getId(), null, 404, "STUDENT_NOT_FOUND");
        expectAssign(teacher, ownStudent.getId(), foreignProgram.getId(), null, 404, "LEARNING_PROGRAM_NOT_FOUND");
        expectAssign(teacher, ownStudent.getId(), draft.getId(), null, 409, "LEARNING_PROGRAM_STATUS_CONFLICT");
        expectAssign(teacher, ownStudent.getId(), archived.getId(), null, 409, "LEARNING_PROGRAM_STATUS_CONFLICT");
        expectAssign(teacher, ownStudent.getId(), active.getId(), 0, 400, "VALIDATION_ERROR");
        expectAssign(teacher, ownStudent.getId(), active.getId(), 120, 201, null);
        expectAssign(teacher, ownStudent.getId(), second.getId(), 240, 201, null);
    }

    @Test
    void createsFirstModuleAndAppendsSequentialModulesWithoutChangingExistingOnes() throws Exception {
        TeacherContext teacher = teacher("module-create@example.com");
        LearningProgramEntity program = program(teacher.teacher, "Modules", LearningProgramStatus.DRAFT);

        String firstResponse = createModule(teacher, program.getId(), "  Основы Python  ", "  Первый раздел  ")
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.title").value("Основы Python"))
            .andExpect(jsonPath("$.description").value("Первый раздел"))
            .andExpect(jsonPath("$.position").value(0))
            .andReturn().getResponse().getContentAsString();
        UUID firstId = UUID.fromString(objectMapper.readTree(firstResponse).get("id").textValue());

        createModule(teacher, program.getId(), "Продолжение", null)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.position").value(1));

        ModuleEntity first = moduleRepository.findById(firstId).orElseThrow();
        assertThat(first.title()).isEqualTo("Основы Python");
        assertThat(first.description()).isEqualTo("Первый раздел");
        assertThat(first.position()).isZero();
    }

    @Test
    void createModuleRejectsForeignArchivedAndAssignedPrograms() throws Exception {
        TeacherContext owner = teacher("module-create-owner@example.com");
        TeacherContext foreign = teacher("module-create-foreign@example.com");
        LearningProgramEntity owned = program(owner.teacher, "Owned", LearningProgramStatus.DRAFT);
        LearningProgramEntity archived = program(owner.teacher, "Archived", LearningProgramStatus.ARCHIVED);
        LearningProgramEntity assigned = program(owner.teacher, "Assigned", LearningProgramStatus.ACTIVE);
        StudentEntity student = student(owner.teacher, "Student");
        studentProgramRepository.saveAndFlush(new StudentProgramEntity(
            UUID.randomUUID(), student.getId(), assigned.getId(), owner.teacher.id(),
            StudentProgramStatus.COMPLETED, 480, Instant.parse("2026-09-01T00:00:00Z"),
            Instant.parse("2026-09-02T00:00:00Z")
        ));

        createModule(foreign, owned.getId(), "Denied", null)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_NOT_FOUND"));
        createModule(owner, archived.getId(), "Denied", null)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_STATUS_CONFLICT"));
        createModule(owner, assigned.getId(), "Denied", null)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_STATUS_CONFLICT"));
    }

    @Test
    void updatesModuleTrimsFieldsAndPreservesPosition() throws Exception {
        TeacherContext teacher = teacher("module-update@example.com");
        LearningProgramEntity program = program(teacher.teacher, "Modules", LearningProgramStatus.DRAFT);
        ModuleEntity module = moduleRepository.saveAndFlush(new ModuleEntity(
            UUID.randomUUID(), program.getId(), "Original", "Original description", 4
        ));

        updateModule(teacher, program.getId(), module.id(), "  New title  ", "  New description  ")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(module.id().toString()))
            .andExpect(jsonPath("$.title").value("New title"))
            .andExpect(jsonPath("$.description").value("New description"))
            .andExpect(jsonPath("$.position").value(4));

        ModuleEntity updated = moduleRepository.findById(module.id()).orElseThrow();
        assertThat(updated.position()).isEqualTo(4);
        assertThat(updated.title()).isEqualTo("New title");

        updateModule(teacher, program.getId(), module.id(), "   ", null)
            .andExpect(status().isBadRequest());
        updateModule(teacher, program.getId(), module.id(), "a".repeat(181), null)
            .andExpect(status().isBadRequest());
    }

    @Test
    void moduleMutationHidesForeignAndMismatchedModulesAndRejectsNonEditablePrograms() throws Exception {
        TeacherContext owner = teacher("module-mutation-owner@example.com");
        TeacherContext foreign = teacher("module-mutation-foreign@example.com");
        LearningProgramEntity owned = program(owner.teacher, "Owned", LearningProgramStatus.DRAFT);
        LearningProgramEntity other = program(owner.teacher, "Other", LearningProgramStatus.DRAFT);
        LearningProgramEntity foreignProgram = program(foreign.teacher, "Foreign", LearningProgramStatus.DRAFT);
        LearningProgramEntity archived = program(owner.teacher, "Archived", LearningProgramStatus.ARCHIVED);
        ModuleEntity module = moduleRepository.saveAndFlush(new ModuleEntity(UUID.randomUUID(), owned.getId(), "Module", null, 0));
        ModuleEntity archivedModule = moduleRepository.saveAndFlush(new ModuleEntity(UUID.randomUUID(), archived.getId(), "Archived", null, 0));

        updateModule(foreign, owned.getId(), module.id(), "Denied", null)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_NOT_FOUND"));
        updateModule(owner, other.getId(), module.id(), "Denied", null)
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_MODULE_NOT_FOUND"));
        deleteModule(owner, foreignProgram.getId(), module.id())
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_NOT_FOUND"));
        deleteModule(owner, archived.getId(), archivedModule.id())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_STATUS_CONFLICT"));
    }

    @Test
    void deletesOnlyEmptyModuleWithoutNormalizingOtherPositions() throws Exception {
        TeacherContext teacher = teacher("module-delete@example.com");
        LearningProgramEntity program = program(teacher.teacher, "Modules", LearningProgramStatus.DRAFT);
        ModuleEntity deleted = moduleRepository.saveAndFlush(new ModuleEntity(UUID.randomUUID(), program.getId(), "Deleted", null, 0));
        ModuleEntity retained = moduleRepository.saveAndFlush(new ModuleEntity(UUID.randomUUID(), program.getId(), "Retained", null, 3));
        ModuleEntity withTopic = moduleRepository.saveAndFlush(new ModuleEntity(UUID.randomUUID(), program.getId(), "With topic", null, 5));
        TopicEntity topic = topicRepository.saveAndFlush(new TopicEntity(
            UUID.randomUUID(), withTopic.id(), "Topic", null, 0, TopicStatus.DRAFT
        ));

        deleteModule(teacher, program.getId(), withTopic.id())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("LEARNING_PROGRAM_MODULE_NOT_EMPTY"));
        assertThat(moduleRepository.findById(withTopic.id())).isPresent();
        assertThat(topicRepository.findById(topic.id())).isPresent();

        deleteModule(teacher, program.getId(), deleted.id())
            .andExpect(status().isNoContent());
        assertThat(moduleRepository.findById(deleted.id())).isEmpty();
        assertThat(moduleRepository.findById(retained.id()).orElseThrow().position()).isEqualTo(3);
    }

    @Test
    void openApiPublishesStableManagementOperationIds() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/teacher/subjects'].get.operationId").value("listTeacherSubjects"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/programs'].get.operationId").value("listTeacherLearningPrograms"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/programs'].post.operationId").value("createTeacherLearningProgram"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/programs/{programId}/modules'].post.operationId").value("createTeacherLearningProgramModule"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/programs/{programId}/modules/{moduleId}'].patch.operationId").value("updateTeacherLearningProgramModule"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/programs/{programId}/modules/{moduleId}'].delete.operationId").value("deleteTeacherLearningProgramModule"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/programs/{programId}'].get.operationId").value("getTeacherLearningProgram"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/programs/{programId}'].patch.operationId").value("updateTeacherLearningProgram"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/programs/{programId}/activate'].post.operationId").value("activateTeacherLearningProgram"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/programs/{programId}/archive'].post.operationId").value("archiveTeacherLearningProgram"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}/programs'].post.operationId").value("assignTeacherStudentProgram"));
    }

    private void expectAssign(TeacherContext teacher, UUID studentId, UUID programId, Integer interval,
                              int expectedStatus, String code) throws Exception {
        String intervalJson = interval == null ? "" : ",\"reportIntervalMinutes\":" + interval;
        var action = mockMvc.perform(post("/api/v1/teacher/students/{id}/programs", studentId)
            .with(user(teacher.principal)).with(csrf()).contentType("application/json")
            .content("{\"learningProgramId\":\"" + programId + "\"" + intervalJson + "}"));
        action.andExpect(status().is(expectedStatus));
        if (code != null) action.andExpect(jsonPath("$.code").value(code));
    }

    private org.springframework.test.web.servlet.ResultActions createModule(
        TeacherContext teacher,
        UUID programId,
        String title,
        String description
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/teacher/programs/{programId}/modules", programId)
            .with(user(teacher.principal)).with(csrf()).contentType("application/json")
            .content(objectMapper.writeValueAsString(new CreateLearningProgramModuleRequest(title, description))));
    }

    private org.springframework.test.web.servlet.ResultActions updateModule(
        TeacherContext teacher, UUID programId, UUID moduleId, String title, String description
    ) throws Exception {
        return mockMvc.perform(patch("/api/v1/teacher/programs/{programId}/modules/{moduleId}", programId, moduleId)
            .with(user(teacher.principal)).with(csrf()).contentType("application/json")
            .content(objectMapper.writeValueAsString(new UpdateLearningProgramModuleRequest(title, description))));
    }

    private org.springframework.test.web.servlet.ResultActions deleteModule(
        TeacherContext teacher, UUID programId, UUID moduleId
    ) throws Exception {
        return mockMvc.perform(delete("/api/v1/teacher/programs/{programId}/modules/{moduleId}", programId, moduleId)
            .with(user(teacher.principal)).with(csrf()));
    }

    private void expectUpdateConflict(
        TeacherContext teacher,
        LearningProgramEntity program,
        String code
    ) throws Exception {
        mockMvc.perform(patch("/api/v1/teacher/programs/{programId}", program.getId())
                .with(user(teacher.principal)).with(csrf()).contentType("application/json")
                .content(updateRequest("Updated", null, program.getVersion())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(code));
    }

    private String updateRequest(String title, String description, Long version) throws Exception {
        return objectMapper.writeValueAsString(new UpdateLearningProgramRequest(title, description, version));
    }

    private TeacherContext teacher(String email) {
        UserEntity user = new UserEntity(UUID.randomUUID(), email, "hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);
        TeacherEntity teacher = teacherRepository.saveAndFlush(new TeacherEntity(UUID.randomUUID(), user, "Teacher"));
        return new TeacherContext(teacher, new AuthenticatedUser(user.id(), email, "hash", true,
            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))));
    }

    private AuthenticatedUser studentPrincipal() {
        return new AuthenticatedUser(UUID.randomUUID(), "student@example.com", "hash", true,
            List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
    }

    private StudentEntity student(TeacherEntity teacher, String name) {
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(UUID.randomUUID(), name, null, StudentStatus.ACTIVE));
        linkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));
        return student;
    }

    private SubjectEntity subject(UUID ownerId, String name, SubjectStatus status) {
        return subjectRepository.saveAndFlush(new SubjectEntity(UUID.randomUUID(), ownerId, null, name, null, status));
    }

    private LearningProgramEntity program(TeacherEntity teacher, String title, LearningProgramStatus status) {
        return learningProgramRepository.saveAndFlush(new LearningProgramEntity(
            UUID.randomUUID(), teacher.id(), PYTHON, title, null, status
        ));
    }

    private record TeacherContext(TeacherEntity teacher, AuthenticatedUser principal) {
    }
}
