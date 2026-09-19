package com.tutorplatform.student.api;

import com.tutorplatform.test.PostgresIntegrationTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tutorplatform.auth.infrastructure.security.AuthenticatedUser;
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentRepository;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.student.domain.TeacherStudentRelationType;
import com.tutorplatform.student.infrastructure.persistence.StudentInviteRepository;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkEntity;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkRepository;
import com.tutorplatform.user.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TeacherStudentApiIntegrationTest extends PostgresIntegrationTest {

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        PostgresIntegrationTest.configurePostgres(registry, "test_teacher_student_api", null);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StudentInviteRepository studentInviteRepository;
    @Autowired
    private TeacherStudentLinkRepository teacherStudentLinkRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanData() {
        studentInviteRepository.deleteAll();
        teacherStudentLinkRepository.deleteAll();
        studentRepository.deleteAll();
        teacherRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createCreatesUnregisteredStudentAndPrimaryLinkForCurrentTeacher() throws Exception {
        TeacherContext teacher = createTeacher("teacher@example.com");

        MvcResult result = mockMvc.perform(post("/api/v1/teacher/students")
                .with(user(teacher.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "firstName": "  Андрей  ",
                      "lastName": "  Иванов  "
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/teacher/students/")))
            .andExpect(jsonPath("$.firstName").value("Андрей"))
            .andExpect(jsonPath("$.lastName").value("Иванов"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.accountStatus").value("UNREGISTERED"))
            .andReturn();

        UUID studentId = UUID.fromString(json(result).required("id").textValue());
        StudentEntity student = studentRepository.findById(studentId).orElseThrow();
        assertThat(student.getUserId()).isNull();
        assertThat(teacherStudentLinkRepository.existsByIdTeacherIdAndIdStudentIdAndRelationTypeAndEndedAtIsNull(teacher.teacher().id(), studentId, TeacherStudentRelationType.PRIMARY)).isTrue();
    }

    @Test
    void listReturnsOnlyCurrentTeachersStudents() throws Exception {
        TeacherContext firstTeacher = createTeacher("first@example.com");
        TeacherContext secondTeacher = createTeacher("second@example.com");
        createStudent(firstTeacher.teacher(), "Андрей", "Иванов");
        createStudent(secondTeacher.teacher(), "Мария", "Петрова");

        mockMvc.perform(get("/api/v1/teacher/students")
                .with(user(firstTeacher.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].firstName").value("Андрей"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listUsesZeroBasedPagination() throws Exception {
        TeacherContext teacher = createTeacher("teacher@example.com");
        createStudent(teacher.teacher(), "Анна", "А");
        createStudent(teacher.teacher(), "Борис", "Б");
        createStudent(teacher.teacher(), "Вера", "В");

        mockMvc.perform(get("/api/v1/teacher/students")
                .with(user(teacher.principal()))
                .param("page", "1")
                .param("size", "2")
                .param("sort", "firstName,asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].firstName").value("Вера"))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/v1/teacher/students")
                .with(user(teacher.principal()))
                .param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void searchMatchesFirstOrLastNameCaseInsensitively() throws Exception {
        TeacherContext teacher = createTeacher("teacher@example.com");
        createStudent(teacher.teacher(), "Андрей", "Иванов");
        createStudent(teacher.teacher(), "Мария", "ПЕТРОВА");
        createStudent(teacher.teacher(), "Сергей", "Сидоров");

        mockMvc.perform(get("/api/v1/teacher/students")
                .with(user(teacher.principal()))
                .param("query", "  петр  "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].firstName").value("Мария"));

        mockMvc.perform(get("/api/v1/teacher/students")
                .with(user(teacher.principal()))
                .param("query", "АНД"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].lastName").value("Иванов"));
    }

    @Test
    void detailReturnsAccountAndPrimaryRelation() throws Exception {
        TeacherContext teacher = createTeacher("teacher@example.com");
        StudentEntity student = createStudent(teacher.teacher(), "Андрей", "Иванов");

        mockMvc.perform(get("/api/v1/teacher/students/{studentId}", student.getId())
                .with(user(teacher.principal())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(student.getId().toString()))
            .andExpect(jsonPath("$.firstName").value("Андрей"))
            .andExpect(jsonPath("$.account.status").value("UNREGISTERED"))
            .andExpect(jsonPath("$.account.email").doesNotExist())
            .andExpect(jsonPath("$.relation.type").value("PRIMARY"))
            .andExpect(jsonPath("$.relation.startedAt").isNotEmpty())
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void updateChangesOnlyProvidedNameFields() throws Exception {
        TeacherContext teacher = createTeacher("teacher@example.com");
        StudentEntity student = createStudent(teacher.teacher(), "Андрей", "Иванов");

        mockMvc.perform(patch("/api/v1/teacher/students/{studentId}", student.getId())
                .with(user(teacher.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"lastName": "  Петров  "}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Андрей"))
            .andExpect(jsonPath("$.lastName").value("Петров"))
            .andExpect(jsonPath("$.accountStatus").value("UNREGISTERED"))
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        StudentEntity updated = studentRepository.findById(student.getId()).orElseThrow();
        assertThat(updated.getFirstName()).isEqualTo("Андрей");
        assertThat(updated.getLastName()).isEqualTo("Петров");

        mockMvc.perform(patch("/api/v1/teacher/students/{studentId}", student.getId())
                .with(user(teacher.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void ownershipMismatchIsNormalizedToStudentNotFound() throws Exception {
        TeacherContext owner = createTeacher("owner@example.com");
        TeacherContext otherTeacher = createTeacher("other@example.com");
        StudentEntity student = createStudent(owner.teacher(), "Андрей", "Иванов");

        mockMvc.perform(get("/api/v1/teacher/students/{studentId}", student.getId())
                .with(user(otherTeacher.principal())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));

        mockMvc.perform(patch("/api/v1/teacher/students/{studentId}", student.getId())
                .with(user(otherTeacher.principal()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"firstName": "Чужое изменение"}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
    }

    @Test
    void unregisteredAccountStatusIsReturnedAndCanBeFiltered() throws Exception {
        TeacherContext teacher = createTeacher("teacher@example.com");
        createStudent(teacher.teacher(), "Андрей", "Иванов");

        mockMvc.perform(get("/api/v1/teacher/students")
                .with(user(teacher.principal()))
                .param("accountStatus", "UNREGISTERED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].accountStatus").value("UNREGISTERED"));

        mockMvc.perform(get("/api/v1/teacher/students")
                .with(user(teacher.principal()))
                .param("accountStatus", "REGISTERED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void openApiPublishesStableTeacherStudentOperationIds() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students'].get.operationId").value("listTeacherStudents"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students'].get.parameters[0].schema.type")
                .value("integer"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students'].get.parameters[1].schema.type")
                .value("integer"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students'].post.operationId").value("createStudent"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}'].get.operationId").value("getStudent"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}'].patch.operationId").value("updateStudent"))
            .andExpect(jsonPath("$.paths['/api/v1/teacher/students/{studentId}'].get.parameters[0].schema.format")
                .value("uuid"))
            .andExpect(jsonPath("$.components.schemas.StudentSummaryResponse.properties.id.format").value("uuid"))
            .andExpect(jsonPath("$.components.schemas.StudentSummaryResponse.properties.createdAt.format")
                .value("date-time"))
            .andExpect(jsonPath("$.components.schemas.StudentSummaryResponse.properties.status.enum.length()")
                .value(3))
            .andExpect(jsonPath("$.components.schemas.StudentSummaryResponse.properties.accountStatus.enum.length()")
                .value(3));
    }

    private TeacherContext createTeacher(String email) {
        UserEntity user = new UserEntity(UUID.randomUUID(), email, "password-hash", UserStatus.ACTIVE);
        user.addRole(UserRole.TEACHER);
        userRepository.saveAndFlush(user);
        TeacherEntity teacher = teacherRepository.saveAndFlush(new TeacherEntity(
            UUID.randomUUID(),
            user,
            "Teacher"
        ));
        AuthenticatedUser principal = new AuthenticatedUser(
            user.id(),
            email,
            "password-hash",
            true,
            List.of(new SimpleGrantedAuthority("ROLE_TEACHER"))
        );
        return new TeacherContext(teacher, principal);
    }

    private StudentEntity createStudent(TeacherEntity teacher, String firstName, String lastName) {
        StudentEntity student = studentRepository.saveAndFlush(new StudentEntity(
            UUID.randomUUID(),
            firstName,
            lastName,
            StudentStatus.ACTIVE
        ));
        teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));
        return student;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private record TeacherContext(TeacherEntity teacher, AuthenticatedUser principal) {
    }
}
