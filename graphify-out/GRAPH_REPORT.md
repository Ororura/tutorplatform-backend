# Graph Report - backend  (2026-09-19)

## Corpus Check
- 714 files · ~117,268 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 4581 nodes · 16464 edges · 190 communities (152 shown, 38 thin omitted)
- Extraction: 88% EXTRACTED · 12% INFERRED · 0% AMBIGUOUS · INFERRED: 2023 edges (avg confidence: 0.81)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `5c467e11`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- org.springframework.http.ResponseEntity
- AuthenticatedUser
- LearningPeriod
- StudentEntity
- AuthIntegrationTest
- HomeworkStatus
- org.junit.jupiter.api.Test
- TaskQuery
- PostgresIntegrationTest
- StudentAccountStatus
- HomeworkEntity
- SkillEntity
- ProgressReport
- org.springframework.transaction.annotation.Transactional
- TeacherProgramManagementApiIntegrationTest
- CodeSubmissionEntity
- .user
- TeacherEntity
- org.springframework.test.context.DynamicPropertyRegistry
- ProgressShareApiIntegrationTest
- SubmissionService
- ProgressReportPdfService
- jakarta.servlet.http.HttpServletResponse
- LessonMaterialEntity
- org.springframework.stereotype.Repository
- DemoDataSeedService.java
- HomeworkApplicationIntegrationTest
- TeacherRegistrationInviteService
- ProgressShare
- TeacherTaskController.java
- io.swagger.v3.oas.annotations.media.Schema
- TaskTestCase
- TeacherLearningProgramService
- RegistrationMode
- StudentProgramEntity
- StudentInviteTokenService
- SubmissionEntity
- HomeworkDatabaseModel
- TeacherAssessmentEntity
- UserEntity
- HomeworkCompletionServiceTest
- HomeworkPersistenceIntegrationTest
- TeacherStudentProgramService
- org.springframework.data.jpa.repository.JpaRepository
- LessonMaterialApplicationIntegrationTest
- FileAssetEntity
- SecurityConfig.java
- SessionApiIntegrationTest
- org.springframework.web.bind.annotation.GetMapping
- SubmissionApiIntegrationTest
- SubmissionPersistenceIntegrationTest
- .run
- StudentTopicProgressStatus
- TaskService
- org.springframework.stereotype.Component
- org.junit.jupiter.params.provider.Arguments
- TaskType
- ProgressReadRepositoryIntegrationTest
- ProgrammingTaskConfig
- TeacherStudentInviteApiIntegrationTest
- StudentDatabaseModel
- DemoDataSeedService
- TeacherRepository
- TeacherDatabaseModel
- ProgressReportSnapshotV1
- .createFixture
- ProgressReportStatus
- ProgressReportPdfModel
- TeacherAssessmentService.java
- HomeworkDetailsResponse
- SubmissionStatus
- ProgramExceptionHandler.java
- GetCurrentProgressService
- ProgressReportApplicationIntegrationTest
- TaskStatus
- LessonMaterialResponse
- AssessmentApiIntegrationTest
- .findStudentProgram
- JpaProgressReportRepository
- StudentInviteEntity
- HttpExecutionAdapter
- TeacherAssessmentDatabaseModel
- SubmissionDatabaseModel
- JpaSubmissionRepository
- AuthenticationSessionService
- LessonMaterialService
- .getId
- AttendanceStatus
- LessonMaterialApiIntegrationTest
- UserRepository
- Writer
- StudentHomeworkApiIntegrationTest
- TeacherAssessmentService
- TopicStatus
- TeacherProgressReportController
- TeacherLessonSessionController.java
- TaskPersistenceIntegrationTest
- ReportShareApiIntegrationTest
- StudentSubmissionController
- LessonSessionService
- PublicCurrentProgressResponse
- TaskDatabaseModel
- TopicTaskDatabaseModel
- LessonMaterialDatabaseModel
- TeacherLearningProgramQuery
- LessonSessionEntity
- SecurityInfrastructureTest
- .progress
- LessonSessionTopicEntity
- TeacherStudentApiIntegrationTest
- TaskTestCaseRepository
- TaskEntity
- ProgressReportApiIntegrationTest
- UpdateProgressReportRequest
- LessonMaterialType
- FileStorageException
- TeacherRegistrationInviteStatus
- .response
- LessonSessionTopicDatabaseModel
- PublicStudentInvitationApiIntegrationTest
- StudentInviteService
- DemoDataProductionGuard
- Русский
- TeacherProgressShareController.java
- ProgressInterval
- HomeworkRepository
- ModuleEntity
- StudentProgramDatabaseModel
- LessonSessionDatabaseModel
- HttpExecutionAdapterTest
- English
- .assign
- ProgressReportPersistenceIntegrationTest
- HttpExecutionAdapterTest.java
- DatabaseBaselineMigrationIntegrationTest
- java.net.URI
- ExecutionProperties
- TeacherAssessmentRepository
- LearningProgramDetailsResponse
- ProgrammingLanguage
- TeacherStudentLinkId
- InvalidLessonMaterialException
- .createModule
- CurrentProgressResponse
- .getInvitation
- JpaSubmissionQuery
- DemoDataSeeder
- ProgressReportNotEditableException
- .createInvite
- JpaStudentRepository
- SecurityProbeController
- ReportShareStatus
- StudentInviteStatus
- Core design decisions
- Основные решения
- .listTasks
- TraceIdFilter
- File lesson materials (MVP)
- .handleInvalidSessionListParameter
- .handleInvalidStudentListParameter
- .handleInvalidSubmission
- .handleInvalidTestCase
- gradlew
- Technology stack
- Testing strategy
- Технологии
- Тестирование
- DemoDataIds
- InvalidProgressReportListParameterException
- InvalidTaskException
- InvalidTaskListParameterException
- TeacherAssessmentConflictException
- InvalidCredentialsException
- LessonMaterialVersionConflictException
- DemoDataAccess
- HomeworkItemPositionConflictException
- HomeworkVersionConflictException
- TeacherInvitationNotFoundException
- RegistrationInviteRequiredException
- InvalidLessonSessionTopicsException
- LessonSessionVersionConflictException
- TaskNotReadyForActivationException
- TaskTopicPositionConflictException
- TaskTypeMismatchException
- TaskVersionConflictException
- AGENTS.md
- StudentInviteNotFoundException

## God Nodes (most connected - your core abstractions)
1. `AuthenticatedUser` - 384 edges
2. `ApiError` - 228 edges
3. `TeacherEntity` - 91 edges
4. `PostgresIntegrationTest` - 88 edges
5. `StudentEntity` - 83 edges
6. `HomeworkStatus` - 77 edges
7. `TaskEntity` - 67 edges
8. `LearningProgramEntity` - 66 edges
9. `TaskApiIntegrationTest` - 66 edges
10. `DemoDataSeedService` - 65 edges

## Surprising Connections (you probably didn't know these)
- `assessmentWith()` --references--> `TeacherAssessmentEntity`  [EXTRACTED]
  src/test/java/com/tutorplatform/assessment/domain/TeacherAssessmentEntityTest.java → src/main/java/com/tutorplatform/assessment/domain/TeacherAssessmentEntity.java
- `scoreFrom()` --references--> `TeacherAssessmentEntity`  [EXTRACTED]
  src/test/java/com/tutorplatform/assessment/domain/TeacherAssessmentEntityTest.java → src/main/java/com/tutorplatform/assessment/domain/TeacherAssessmentEntity.java
- `TeacherAssessmentController` --implements--> `TeacherAssessmentApi`  [EXTRACTED]
  src/main/java/com/tutorplatform/assessment/api/TeacherAssessmentController.java → src/main/java/com/tutorplatform/assessment/api/TeacherAssessmentApi.java
- `TeacherAssessmentService` --references--> `TeacherAssessmentRepository`  [EXTRACTED]
  src/main/java/com/tutorplatform/assessment/application/TeacherAssessmentService.java → src/main/java/com/tutorplatform/assessment/domain/TeacherAssessmentRepository.java
- `TeacherAssessmentService` --references--> `LessonSessionQuery`  [EXTRACTED]
  src/main/java/com/tutorplatform/assessment/application/TeacherAssessmentService.java → src/main/java/com/tutorplatform/session/application/LessonSessionQuery.java

## Import Cycles
- None detected.

## Communities (190 total, 38 thin omitted)

### Community 0 - "org.springframework.http.ResponseEntity"
Cohesion: 0.04
Nodes (35): jakarta.validation.ConstraintViolationException, org.springframework.core.annotation.Order, org.springframework.http.converter.HttpMessageNotReadableException, org.springframework.http.HttpStatus, org.springframework.http.ResponseEntity, org.springframework.web.bind.annotation.ExceptionHandler, org.springframework.web.bind.annotation.RestControllerAdvice, org.springframework.web.bind.MethodArgumentNotValidException (+27 more)

### Community 1 - "AuthenticatedUser"
Cohesion: 0.05
Nodes (32): io.swagger.v3.oas.annotations.Operation, io.swagger.v3.oas.annotations.responses.ApiResponses, org.springframework.security.core.GrantedAuthority, org.springframework.security.core.userdetails.UserDetails, TeacherAssessmentApi, AuthApi, CsrfTokenApi, AuthenticatedUser (+24 more)

### Community 2 - "LearningPeriod"
Cohesion: 0.05
Nodes (25): org.springframework.transaction.event.TransactionalEventListener, HistoricalLearningPeriodChangeException, LearningPeriodStudentProgramNotFoundException, LearningPeriodService, Timeline, LessonSessionLearningPeriodListener, LearningPeriod, LearningPeriodRepository (+17 more)

### Community 3 - "StudentEntity"
Cohesion: 0.08
Nodes (27): jakarta.persistence.EntityManagerFactory, LearningProgramEntity, LearningProgramStatus, ACTIVE, ARCHIVED, DRAFT, StudentProgramStatus, ACTIVE (+19 more)

### Community 4 - "AuthIntegrationTest"
Cohesion: 0.07
Nodes (11): jakarta.servlet.http.Cookie, org.junit.jupiter.api.BeforeEach, org.springframework.test.context.ActiveProfiles, Override, AuthIntegrationTest, CsrfExchange, CsrfExchange, DemoDataSeederIntegrationTest (+3 more)

### Community 5 - "HomeworkStatus"
Cohesion: 0.05
Nodes (26): HomeworkSummaryResponse, StudentHomeworkDetailsResponse, StudentHomeworkPageResponse, StudentHomeworkSummaryResponse, GetMapping, Override, RequestMapping, RestController (+18 more)

### Community 6 - "org.junit.jupiter.api.Test"
Cohesion: 0.06
Nodes (8): org.junit.jupiter.api.Test, FileMaterialPolicyTest, ExecutionSecurityBoundaryTest, LocalFileStorageTest, FoundationConventionsTest, HomeworkApiIntegrationTest, ProgressReportSnapshotJsonCodecTest, SubmissionReviewedEventTest

### Community 7 - "TaskQuery"
Cohesion: 0.07
Nodes (20): org.mockito.junit.jupiter.MockitoSettings, ExecutionPort, HomeworkQuery, StudentTaskContext, RequestMapping, RestController, StudentRunCodeController, Reason (+12 more)

### Community 8 - "PostgresIntegrationTest"
Cohesion: 0.12
Nodes (21): com.fasterxml.jackson.databind.JsonNode, com.fasterxml.jackson.databind.ObjectMapper, java.sql.Connection, org.junit.jupiter.api.AfterAll, org.junit.jupiter.api.BeforeAll, org.junit.jupiter.params.ParameterizedTest, org.junit.jupiter.params.provider.EnumSource, org.springframework.boot.test.context.SpringBootTest (+13 more)

### Community 9 - "StudentAccountStatus"
Cohesion: 0.07
Nodes (27): CreateStudentRequest, StudentAccountResponse, StudentDetailsResponse, StudentPageResponse, RelationType, PRIMARY, StudentRelationResponse, StudentSummaryResponse (+19 more)

### Community 10 - "HomeworkEntity"
Cohesion: 0.10
Nodes (11): HomeworkItemResponse, HomeworkTaskNotAssignableException, HomeworkTaskSubjectMismatchException, HomeworkItemResult, HomeworkResult, HomeworkService, SortParameters, ValidatedItems (+3 more)

### Community 11 - "SkillEntity"
Cohesion: 0.06
Nodes (18): SkillEntity, SkillRepository, TaskSkillEntity, TaskSkillRepository, Override, JpaSkillRepository, Entity, Table (+10 more)

### Community 12 - "ProgressReport"
Cohesion: 0.08
Nodes (17): CreateProgressReportDraft, EditProgressReportDraft, EditProgressReportDraftCommand, ProgressReportConflictException, ProgressReportNotFoundException, ProgressReportVersionConflictException, AuthorizedProgram, ProgressReportAuthorization (+9 more)

### Community 13 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.08
Nodes (13): org.slf4j.Logger, org.springframework.context.ApplicationEventPublisher, org.springframework.stereotype.Service, org.springframework.transaction.annotation.Transactional, HomeworkNotFoundException, ProgramQuery, ProgressStudentProgramNotFoundException, InstantPrecision (+5 more)

### Community 14 - "TeacherProgramManagementApiIntegrationTest"
Cohesion: 0.14
Nodes (3): ResultActions, TeacherContext, TeacherProgramManagementApiIntegrationTest

### Community 15 - "CodeSubmissionEntity"
Cohesion: 0.08
Nodes (19): CodeSubmissionExecutionResponse, CodeSubmissionResult, CodeExecutionStatus, FAILED, PASSED, PENDING, RUNNING, RUNTIME_ERROR (+11 more)

### Community 16 - ".user"
Cohesion: 0.17
Nodes (5): AttachBody, CreateBody, TaskApiIntegrationTest, TaskFixture, UpdateBody

### Community 17 - "TeacherEntity"
Cohesion: 0.10
Nodes (8): MockMultipartHttpServletRequestBuilder, TopicEntity, TeacherEntity, ContentFixture, ContentFixture, SessionFixture, Fixture, ResultActions

### Community 19 - "ProgressShareApiIntegrationTest"
Cohesion: 0.11
Nodes (5): Fixture, ProgressApiIntegrationTest, Fixture, ResultActions, ProgressShareApiIntegrationTest

### Community 20 - "SubmissionService"
Cohesion: 0.07
Nodes (15): GetMapping, Override, PatchMapping, RequestMapping, RestController, TeacherSubmissionController, TeacherSubmissionPageResponse, TeacherSubmissionResponse (+7 more)

### Community 21 - "ProgressReportPdfService"
Cohesion: 0.08
Nodes (17): ProgressReportPdfResponse, Override, PublicProgressReportController, Assessment, Metrics, PublicProgressReportResponse, PublicProgressReportSnapshot, Skill (+9 more)

### Community 22 - "jakarta.servlet.http.HttpServletResponse"
Cohesion: 0.09
Nodes (19): jakarta.servlet.http.HttpServletRequest, jakarta.servlet.http.HttpServletResponse, AcceptTeacherInvitationRequest, GetMapping, Override, PostMapping, ResponseStatus, CurrentUserResponse (+11 more)

### Community 24 - "org.springframework.stereotype.Repository"
Cohesion: 0.18
Nodes (22): jakarta.persistence.EntityManager, org.flywaydb.core.Flyway, org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest, org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase, org.springframework.context.annotation.Import, org.springframework.stereotype.Repository, org.springframework.transaction.PlatformTransactionManager, org.springframework.transaction.support.TransactionTemplate (+14 more)

### Community 25 - "DemoDataSeedService.java"
Cohesion: 0.11
Nodes (9): ProgramQueryService, LearningProgramRepository, ModuleRepository, StudentProgramRepository, StudentTopicProgressRepository, TopicRepository, LessonSessionRepository, SessionApplicationIntegrationTest (+1 more)

### Community 26 - "HomeworkApplicationIntegrationTest"
Cohesion: 0.14
Nodes (6): CreateHomeworkCommand, HomeworkItemInput, UpdateHomeworkCommand, Fixture, HomeworkApplicationIntegrationTest, TeacherFixture

### Community 27 - "TeacherRegistrationInviteService"
Cohesion: 0.09
Nodes (15): AdminTeacherInvitationController, GetMapping, RequestMapping, RestController, RequestMapping, RestController, PublicTeacherInvitationController, TeacherInvitationCreatedResponse (+7 more)

### Community 28 - "ProgressShare"
Cohesion: 0.09
Nodes (13): ProgressShareSummaryResponse, ProgressShareNotFoundException, ProgressShareService, ProgressShare, ProgressShareRepository, ProgressShareStatus, ACTIVE, EXPIRED (+5 more)

### Community 29 - "TeacherTaskController.java"
Cohesion: 0.10
Nodes (19): ProgrammingTaskConfigResponse, UpdateProgrammingTaskConfigRequest, TaskResponse, TeacherTaskApi, Override, PatchMapping, PostMapping, PutMapping (+11 more)

### Community 30 - "io.swagger.v3.oas.annotations.media.Schema"
Cohesion: 0.07
Nodes (21): com.fasterxml.jackson.annotation.JsonAnySetter, com.fasterxml.jackson.annotation.JsonInclude, io.swagger.v3.oas.annotations.media.Schema, UserRole, ADMIN, STUDENT, TEACHER, RunCodeExecutionStatus (+13 more)

### Community 31 - "TaskTestCase"
Cohesion: 0.08
Nodes (12): TaskSubjectNotFoundException, TaskResult, ComparisonMode, EXACT, NORMALIZED, TaskTestCase, Override, JpaTaskTestCaseRepository (+4 more)

### Community 32 - "TeacherLearningProgramService"
Cohesion: 0.12
Nodes (9): org.springframework.web.bind.annotation.DeleteMapping, org.springframework.web.bind.annotation.PatchMapping, org.springframework.web.bind.annotation.PostMapping, CreateLearningProgramRequest, CreateLearningProgramTopicRequest, LearningProgramSummaryResponse, Override, TeacherLearningProgramController (+1 more)

### Community 33 - "RegistrationMode"
Cohesion: 0.10
Nodes (15): AdminPlatformSettingsController, GetMapping, PatchMapping, RequestMapping, RestController, ChangeRegistrationModeRequest, PlatformSettingsResponse, RegistrationSettingsResponse (+7 more)

### Community 34 - "StudentProgramEntity"
Cohesion: 0.13
Nodes (6): Override, StudentProgramEntity, Override, JpaStudentProgramRepository, ProgramFixture, ProgramPersistenceIntegrationTest

### Community 35 - "StudentInviteTokenService"
Cohesion: 0.10
Nodes (11): CreateReportShareRequest, ReportShareCreatedResponse, Override, InvalidReportShareExpirationException, ReportShareNotAllowedException, ReportShareNotFoundException, ReportShareService, ReportShare (+3 more)

### Community 36 - "SubmissionEntity"
Cohesion: 0.14
Nodes (5): InvalidSubmissionReviewStatusException, SubmissionNotFoundException, SubmissionNotReviewableException, SubmissionEntity, ResultActions

### Community 37 - "HomeworkDatabaseModel"
Cohesion: 0.09
Nodes (11): org.springframework.data.jpa.repository.EntityGraph, HomeworkDatabaseModel, Entity, Table, HomeworkDatabaseRepository, HomeworkItemDatabaseModel, Entity, Table (+3 more)

### Community 39 - "UserEntity"
Cohesion: 0.09
Nodes (16): Override, UserEntity, UserRole, ADMIN, PARENT, STUDENT, TEACHER, UserStatus (+8 more)

### Community 42 - "TeacherStudentProgramService"
Cohesion: 0.13
Nodes (13): java.sql.ResultSet, ProgramModule, ProgramSubject, ProgramTopic, StudentProgramDetails, StudentProgramSummary, TeacherStudentProgramQuery, TeacherStudentProgramService (+5 more)

### Community 43 - "org.springframework.data.jpa.repository.JpaRepository"
Cohesion: 0.10
Nodes (8): org.springframework.data.jpa.repository.JpaRepository, org.springframework.data.jpa.repository.Modifying, org.springframework.data.jpa.repository.Query, Override, ModuleDatabaseRepository, StudentTopicProgressDatabaseRepository, TopicDatabaseRepository, SubjectDatabaseRepository

### Community 44 - "LessonMaterialApplicationIntegrationTest"
Cohesion: 0.18
Nodes (5): CreateLessonMaterialCommand, LessonMaterialResult, CapturedOutput, ContentFixture, LessonMaterialApplicationIntegrationTest

### Community 45 - "FileAssetEntity"
Cohesion: 0.10
Nodes (11): FileAssetEntity, FileAssetRepository, StorageProvider, LOCAL, S3, FileAssetDatabaseModel, Entity, Table (+3 more)

### Community 46 - "SecurityConfig.java"
Cohesion: 0.13
Nodes (15): io.swagger.v3.oas.models.OpenAPI, OpenAPI, org.springdoc.core.customizers.OpenApiCustomizer, org.springframework.boot.context.properties.EnableConfigurationProperties, org.springframework.context.annotation.Bean, org.springframework.context.annotation.Configuration, org.springframework.security.config.annotation.web.builders.HttpSecurity, org.springframework.security.config.annotation.web.configuration.EnableWebSecurity (+7 more)

### Community 47 - "SessionApiIntegrationTest"
Cohesion: 0.21
Nodes (3): org.junit.jupiter.params.provider.ValueSource, SessionApiIntegrationTest, SessionFixture

### Community 48 - "org.springframework.web.bind.annotation.GetMapping"
Cohesion: 0.16
Nodes (14): org.springframework.security.web.csrf.CsrfToken, org.springframework.web.bind.annotation.GetMapping, org.springframework.web.bind.annotation.RequestMapping, org.springframework.web.bind.annotation.RestController, CsrfController, CsrfTokenResponse, PublicRegistrationController, TeacherStudentProgramController (+6 more)

### Community 49 - "SubmissionApiIntegrationTest"
Cohesion: 0.23
Nodes (4): org.springframework.test.web.servlet.ResultActions, Fixture, Fixture, SubmissionApiIntegrationTest

### Community 50 - "SubmissionPersistenceIntegrationTest"
Cohesion: 0.18
Nodes (7): Fixture, ForeignKey, HOMEWORK_ITEM, STUDENT, STUDENT_PROGRAM, TASK, SubmissionPersistenceIntegrationTest

### Community 51 - ".run"
Cohesion: 0.12
Nodes (11): ExecutionRequest, ExecutionResult, ExecutionStatus, FAILED, PASSED, RUNTIME_ERROR, SYSTEM_ERROR, TIMEOUT (+3 more)

### Community 52 - "StudentTopicProgressStatus"
Cohesion: 0.10
Nodes (12): StudentTopicProgressEntity, StudentTopicProgressStatus, AVAILABLE, COMPLETED, IN_PROGRESS, LOCKED, Override, Entity (+4 more)

### Community 53 - "TaskService"
Cohesion: 0.10
Nodes (7): InvalidProgrammingTaskConfigException, TaskAlreadyAttachedException, TaskSubjectMismatchException, TaskTopicNotFoundException, SortParameters, TaskService, TopicTaskResult

### Community 54 - "org.springframework.stereotype.Component"
Cohesion: 0.12
Nodes (12): java.security.SecureRandom, org.springframework.security.access.AccessDeniedException, org.springframework.security.core.AuthenticationException, org.springframework.security.web.access.AccessDeniedHandler, org.springframework.security.web.AuthenticationEntryPoint, org.springframework.stereotype.Component, TeacherRegistrationInviteTokenService, Token (+4 more)

### Community 55 - "org.junit.jupiter.params.provider.Arguments"
Cohesion: 0.11
Nodes (11): org.junit.jupiter.params.provider.Arguments, assessment(), assessmentWith(), Override, ScoreField, HOMEWORK, INDEPENDENCE, PRACTICE (+3 more)

### Community 56 - "TaskType"
Cohesion: 0.12
Nodes (15): StudentHomeworkItemResponse, CodeExecutionResponse, StudentTaskResponse, CodeExecution, Item, StudentHomeworkDetails, Task, Override (+7 more)

### Community 57 - "ProgressReadRepositoryIntegrationTest"
Cohesion: 0.23
Nodes (4): PracticeMetrics, Fixture, Offset, ProgressReadRepositoryIntegrationTest

### Community 58 - "ProgrammingTaskConfig"
Cohesion: 0.11
Nodes (10): ProgrammingTaskConfig, ProgrammingTaskConfigRepository, Override, JpaProgrammingTaskConfigRepository, Entity, Table, ProgrammingTaskConfigDatabaseModel, ProgrammingTaskConfigDatabaseRepository (+2 more)

### Community 59 - "TeacherStudentInviteApiIntegrationTest"
Cohesion: 0.17
Nodes (9): org.junit.jupiter.api.extension.ExtendWith, org.springframework.boot.test.system.CapturedOutput, org.springframework.boot.test.system.OutputCaptureExtension, ApiExceptionHandlerTest, TeacherContext, ResultActions, UserRole, TeacherContext (+1 more)

### Community 60 - "StudentDatabaseModel"
Cohesion: 0.10
Nodes (9): org.springframework.data.jpa.repository.Lock, Entity, Table, LearningProgramDatabaseModel, LearningProgramDatabaseRepository, Entity, Table, StudentDatabaseModel (+1 more)

### Community 61 - "DemoDataSeedService"
Cohesion: 0.17
Nodes (6): DemoDataSeedService, SeedCounts, SeedPeople, SeedPrograms, SeedResult, TestData

### Community 62 - "TeacherRepository"
Cohesion: 0.12
Nodes (10): Override, StudentOwnershipQueryService, TeacherStudentRelationType, ASSISTANT, PRIMARY, Entity, Table, TeacherStudentLinkEntity (+2 more)

### Community 63 - "TeacherDatabaseModel"
Cohesion: 0.09
Nodes (8): Entity, Table, SubjectDatabaseModel, Override, Entity, Table, TeacherDatabaseModel, TeacherDatabaseRepository

### Community 64 - "ProgressReportSnapshotV1"
Cohesion: 0.13
Nodes (11): ProgressReportPdfNotAvailableException, ProgressReportPdfModelFactory, ProgressReportSnapshotSchemas, Assessment, Metrics, ProgressReportSnapshotV1, Skill, Topic (+3 more)

### Community 65 - ".createFixture"
Cohesion: 0.17
Nodes (4): LessonSessionTopicInput, UpdateLessonSessionCommand, SessionFixture, TeacherFixture

### Community 66 - "ProgressReportStatus"
Cohesion: 0.13
Nodes (12): org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate, ProgressReportSummaryResponse, ProgressReportReadQuery, ProgressReportSummary, ProgressReportSummaryPage, ProgressReportSummaryPageResult, ProgressReportStatus, ARCHIVED (+4 more)

### Community 67 - "ProgressReportPdfModel"
Cohesion: 0.14
Nodes (10): PDDocument, Assessment, Metrics, ProgressReportPdfModel, Skill, Topic, Topic, PdfBoxProgressReportPdfRenderer (+2 more)

### Community 68 - "TeacherAssessmentService.java"
Cohesion: 0.12
Nodes (7): InvalidTeacherAssessmentScoreException, LessonSessionNotFoundException, LessonSessionContext, LessonSessionPage, LessonSessionQuery, Override, JpaLessonSessionQuery

### Community 69 - "HomeworkDetailsResponse"
Cohesion: 0.17
Nodes (12): CreateHomeworkRequest, HomeworkItemRequest, UpdateHomeworkRequest, HomeworkDetailsResponse, HomeworkPageResponse, GetMapping, Override, PatchMapping (+4 more)

### Community 70 - "SubmissionStatus"
Cohesion: 0.14
Nodes (9): SubmissionReviewedHomeworkListener, SubmissionReviewedEvent, SubmissionStatus, FAILED, NEEDS_REVIEW, PASSED, SUBMITTED, SYSTEM_ERROR (+1 more)

### Community 71 - "ProgramExceptionHandler.java"
Cohesion: 0.08
Nodes (10): InvalidLearningProgramStatusException, LearningProgramModuleNotEmptyException, LearningProgramModuleNotFoundException, LearningProgramNotFoundException, LearningProgramTopicNotFoundException, LearningProgramTopicVersionConflictException, LearningProgramVersionConflictException, StudentProgramAlreadyAssignedException (+2 more)

### Community 72 - "GetCurrentProgressService"
Cohesion: 0.17
Nodes (7): GetCurrentProgressService, ProgressReadRepository, AssessmentAverages, CurrentProgress, HomeworkMetrics, ProgressCalculator, TopicProgress

### Community 73 - "ProgressReportApplicationIntegrationTest"
Cohesion: 0.19
Nodes (5): InvalidProgressReportPeriodException, Fixture, Facts, Fixture, ProgressReportApplicationIntegrationTest

### Community 74 - "TaskStatus"
Cohesion: 0.13
Nodes (12): UpdateTaskRequest, CreateTaskCommand, TaskPage, UpdateTaskCommand, TaskDifficulty, EASY, HARD, MEDIUM (+4 more)

### Community 75 - "LessonMaterialResponse"
Cohesion: 0.19
Nodes (12): org.springframework.web.multipart.MultipartFile, CreateLessonMaterialRequest, UpdateLessonMaterialRequest, LessonMaterialResponse, TeacherLessonMaterialApi, GetMapping, Override, PatchMapping (+4 more)

### Community 76 - "AssessmentApiIntegrationTest"
Cohesion: 0.23
Nodes (3): SaveTeacherAssessmentCommand, AssessmentApiIntegrationTest, Fixture

### Community 77 - ".findStudentProgram"
Cohesion: 0.16
Nodes (7): HomeworkStudentProgramNotFoundException, Override, StudentProgramContext, StudentProgramNotFoundException, HomeworkItemNotFoundException, SubmissionContextInvalidException, HomeworkSubmissionContext

### Community 78 - "JpaProgressReportRepository"
Cohesion: 0.15
Nodes (7): Override, JpaProgressReportRepository, Entity, Table, ProgressReportDatabaseModel, ProgressReportDatabaseRepository, ProgressReportSnapshotJsonCodec

### Community 79 - "StudentInviteEntity"
Cohesion: 0.12
Nodes (7): PublicStudentInviteAlreadyAcceptedException, StudentInviteAlreadyAcceptedException, StudentInviteExpiredException, StudentInviteRevokedException, Entity, Table, StudentInviteEntity

### Community 80 - "HttpExecutionAdapter"
Cohesion: 0.12
Nodes (9): Builder, org.springframework.web.client.RestClient, BoundedOutput, HttpExecutionAdapter, Override, TestCase, WorkerExecutionRequest, TestResult (+1 more)

### Community 81 - "TeacherAssessmentDatabaseModel"
Cohesion: 0.15
Nodes (7): jakarta.persistence.Entity, jakarta.persistence.Table, TeacherAssessmentDatabaseModel, Override, JpaReportShareRepository, ReportShareDatabaseModel, ReportShareDatabaseRepository

### Community 82 - "SubmissionDatabaseModel"
Cohesion: 0.17
Nodes (6): org.springframework.data.domain.Page, org.springframework.data.domain.Pageable, Entity, Table, SubmissionDatabaseModel, SubmissionDatabaseRepository

### Community 83 - "JpaSubmissionRepository"
Cohesion: 0.17
Nodes (6): org.springframework.data.domain.Sort, SubmissionAttemptContext, SubmissionPage, Override, Page, JpaSubmissionRepository

### Community 84 - "AuthenticationSessionService"
Cohesion: 0.17
Nodes (12): org.springframework.security.authentication.AuthenticationManager, org.springframework.security.web.authentication.session.SessionAuthenticationStrategy, AuthController, RequestMapping, RestController, RequestMapping, RestController, PublicTeacherInvitationAcceptanceController (+4 more)

### Community 85 - "LessonMaterialService"
Cohesion: 0.13
Nodes (7): LessonMaterialNotFoundException, LessonMaterialPositionConflictException, TopicNotFoundException, Download, FileMaterialService, LessonMaterialService, TopicContext

### Community 87 - "AttendanceStatus"
Cohesion: 0.12
Nodes (10): LessonSessionSummaryResponse, LessonSessionTopicResponse, CreateLessonSessionCommand, LessonSessionPageResult, LessonSessionResult, LessonSessionTopicResult, AttendanceStatus, ATTENDED (+2 more)

### Community 88 - "LessonMaterialApiIntegrationTest"
Cohesion: 0.24
Nodes (3): ContentFixture, LessonMaterialApiIntegrationTest, MaterialRequestBody

### Community 89 - "UserRepository"
Cohesion: 0.12
Nodes (6): java.sql.Timestamp, org.springframework.jdbc.core.simple.JdbcClient, org.springframework.security.core.userdetails.UserDetailsService, CurrentUserQueryRepository, DatabaseUserDetailsService, UserRepository

### Community 90 - "Writer"
Cohesion: 0.19
Nodes (6): org.apache.pdfbox.pdmodel.font.PDFont, org.apache.pdfbox.pdmodel.PDDocument, org.apache.pdfbox.pdmodel.PDPageContentStream, PDPageContentStream, Override, Writer

### Community 91 - "StudentHomeworkApiIntegrationTest"
Cohesion: 0.23
Nodes (3): org.hibernate.stat.Statistics, Fixture, StudentHomeworkApiIntegrationTest

### Community 92 - "TeacherAssessmentService"
Cohesion: 0.13
Nodes (11): SaveTeacherAssessmentRequest, GetMapping, Override, PutMapping, RequestMapping, RestController, TeacherAssessmentController, TeacherAssessmentResponse (+3 more)

### Community 93 - "TopicStatus"
Cohesion: 0.13
Nodes (10): LearningProgramTopicDetailsResponse, ProgramTopicResponse, UpdateLearningProgramTopicRequest, TopicStatus, ACTIVE, ARCHIVED, DRAFT, Entity (+2 more)

### Community 94 - "TeacherProgressReportController"
Cohesion: 0.20
Nodes (11): CreateProgressReportRequest, PublishProgressReportRequest, ProgressReportDetailsResponse, ProgressReportPageResponse, TeacherProgressReportApi, GetMapping, Override, PostMapping (+3 more)

### Community 95 - "TeacherLessonSessionController.java"
Cohesion: 0.19
Nodes (12): CreateLessonSessionRequest, LessonSessionTopicRequest, UpdateLessonSessionRequest, LessonSessionDetailsResponse, LessonSessionPageResponse, GetMapping, Override, PatchMapping (+4 more)

### Community 97 - "ReportShareApiIntegrationTest"
Cohesion: 0.25
Nodes (3): Fixture, ResultActions, ReportShareApiIntegrationTest

### Community 98 - "StudentSubmissionController"
Cohesion: 0.20
Nodes (11): StudentSubmissionApi, GetMapping, Override, PostMapping, RequestMapping, RestController, StudentSubmissionController, StudentSubmissionPageResponse (+3 more)

### Community 99 - "LessonSessionService"
Cohesion: 0.22
Nodes (3): TopicOutsideStudentProgramException, LessonSessionService, SortParameters

### Community 100 - "PublicCurrentProgressResponse"
Cohesion: 0.16
Nodes (10): Override, PublicAssessmentResponse, PublicCurrentProgressResponse, PublicHomeworkResponse, PublicPracticeResponse, PublicTopicResponse, PublicTopicsResponse, ProgressShareExpiredException (+2 more)

### Community 101 - "TaskDatabaseModel"
Cohesion: 0.14
Nodes (6): Override, JpaTaskRepository, Entity, Table, TaskDatabaseModel, TaskDatabaseRepository

### Community 102 - "TopicTaskDatabaseModel"
Cohesion: 0.13
Nodes (6): Entity, Table, TopicTaskDatabaseModel, TopicTaskDatabaseRepository, Override, TopicTaskId

### Community 103 - "LessonMaterialDatabaseModel"
Cohesion: 0.17
Nodes (6): Override, JpaLessonMaterialRepository, Entity, Table, LessonMaterialDatabaseModel, LessonMaterialDatabaseRepository

### Community 104 - "TeacherLearningProgramQuery"
Cohesion: 0.19
Nodes (10): LearningProgramDetails, LearningProgramSummary, ModuleDetails, TeacherLearningProgramQuery, TopicDetails, Override, JdbcTeacherLearningProgramQuery, ModuleRow (+2 more)

### Community 105 - "LessonSessionEntity"
Cohesion: 0.16
Nodes (4): LessonSessionEntity, Override, JpaLessonSessionRepository, TransactionTemplate

### Community 106 - "SecurityInfrastructureTest"
Cohesion: 0.17
Nodes (7): HttpMethod, org.junit.jupiter.params.provider.MethodSource, org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest, org.springframework.mock.web.MockHttpSession, SecurityInfrastructureTest.SecurityProbeController, CsrfExchange, SecurityInfrastructureTest

### Community 107 - ".progress"
Cohesion: 0.29
Nodes (3): SessionMetrics, Offset, ProgressCalculatorTest

### Community 108 - "LessonSessionTopicEntity"
Cohesion: 0.19
Nodes (4): LessonSessionTopicEntity, LessonSessionTopicRepository, Override, JpaLessonSessionTopicRepository

### Community 110 - "TaskTestCaseRepository"
Cohesion: 0.16
Nodes (5): TaskTestCaseRepository, TopicTaskEntity, TopicTaskRepository, Override, JpaTopicTaskRepository

### Community 112 - "ProgressReportApiIntegrationTest"
Cohesion: 0.26
Nodes (3): Fixture, ResultActions, ProgressReportApiIntegrationTest

### Community 113 - "UpdateProgressReportRequest"
Cohesion: 0.18
Nodes (5): com.fasterxml.jackson.annotation.JsonIgnore, jakarta.validation.constraints.AssertTrue, UpdateProgressReportRequest, PatchMapping, UpdateStudentRequest

### Community 114 - "LessonMaterialType"
Cohesion: 0.15
Nodes (8): UpdateLessonMaterialCommand, LessonMaterialType, CODE_EXAMPLE, FILE, IMAGE, LINK, MARKDOWN, TEXT

### Community 115 - "FileStorageException"
Cohesion: 0.21
Nodes (5): FileStorage, StoredObject, FileStorageException, Override, LocalFileStorage

### Community 116 - "TeacherRegistrationInviteStatus"
Cohesion: 0.15
Nodes (9): PublicTeacherInvitationResponse, TeacherInvitationListResponse, TeacherInvitationSummaryResponse, PublicInvitation, TeacherRegistrationInviteStatus, ACCEPTED, ACTIVE, EXPIRED (+1 more)

### Community 118 - "LessonSessionTopicDatabaseModel"
Cohesion: 0.17
Nodes (6): Entity, Table, LessonSessionTopicDatabaseModel, LessonSessionTopicDatabaseRepository, Override, LessonSessionTopicId

### Community 119 - "PublicStudentInvitationApiIntegrationTest"
Cohesion: 0.32
Nodes (3): CsrfExchange, ResultActions, PublicStudentInvitationApiIntegrationTest

### Community 120 - "StudentInviteService"
Cohesion: 0.17
Nodes (8): StudentInviteListResponse, DeleteMapping, GetMapping, Override, RequestMapping, RestController, TeacherStudentInviteController, StudentInviteService

### Community 121 - "DemoDataProductionGuard"
Cohesion: 0.19
Nodes (8): org.springframework.boot.autoconfigure.SpringBootApplication, org.springframework.boot.EnvironmentPostProcessor, org.springframework.boot.SpringApplication, org.springframework.core.env.ConfigurableEnvironment, DemoDataProductionGuard, Override, TutorLearningPlatformApplication, DemoDataProductionGuardTest

### Community 122 - "Русский"
Cohesion: 0.13
Nodes (14): API contract, Backend-модули, Tutor Learning Platform, Архитектура, Аутентификация и безопасность, Документация, Запуск, Модель данных (+6 more)

### Community 123 - "TeacherProgressShareController.java"
Cohesion: 0.20
Nodes (6): CreateProgressShareRequest, ProgressShareCreatedResponse, ProgressShareListResponse, Override, TeacherProgressShareController, InvalidProgressShareExpirationException

### Community 124 - "ProgressInterval"
Cohesion: 0.23
Nodes (4): ProgressInterval, Override, JdbcProgressReadRepository, LearningPeriodNotFoundException

### Community 125 - "HomeworkRepository"
Cohesion: 0.21
Nodes (5): org.mockito.junit.jupiter.MockitoExtension, HomeworkCompletionService, HomeworkRepository, HomeworkItemSubmissionState, SubmissionQuery

### Community 126 - "ModuleEntity"
Cohesion: 0.21
Nodes (5): ModuleEntity, Override, Entity, Table, ModuleDatabaseModel

### Community 127 - "StudentProgramDatabaseModel"
Cohesion: 0.16
Nodes (4): Entity, Table, StudentProgramDatabaseModel, StudentProgramDatabaseRepository

### Community 128 - "LessonSessionDatabaseModel"
Cohesion: 0.21
Nodes (5): Override, Entity, Table, LessonSessionDatabaseModel, LessonSessionDatabaseRepository

### Community 130 - "English"
Cohesion: 0.15
Nodes (13): API contract, Architecture, Authentication and security, Backend modules, Data model, Development, Documentation, English (+5 more)

### Community 131 - ".assign"
Cohesion: 0.21
Nodes (3): AssignStudentProgramRequest, StudentProgramSummaryResponse, Override

### Community 133 - "HttpExecutionAdapterTest.java"
Cohesion: 0.17
Nodes (8): com.sun.net.httpserver.HttpExchange, com.sun.net.httpserver.HttpServer, org.junit.jupiter.api.AfterEach, ExecutionComparisonMode, EXACT, NORMALIZED, ExecutionLanguage, PYTHON

### Community 134 - "DatabaseBaselineMigrationIntegrationTest"
Cohesion: 0.32
Nodes (4): Entry, DatabaseBaselineMigrationIntegrationTest, ExpectedIndex, IndexMetadata

### Community 135 - "java.net.URI"
Cohesion: 0.30
Nodes (3): java.net.URI, ProductionInfrastructureConfiguration, ProductionInfrastructureConfigurationTest

### Community 136 - "ExecutionProperties"
Cohesion: 0.21
Nodes (6): org.springframework.boot.context.properties.ConfigurationProperties, org.springframework.validation.annotation.Validated, ExecutionProperties, Output, Worker, ExecutionPropertiesTest

### Community 137 - "TeacherAssessmentRepository"
Cohesion: 0.24
Nodes (4): TeacherAssessmentRepository, Override, JpaTeacherAssessmentRepository, TeacherAssessmentDatabaseRepository

### Community 138 - "LearningProgramDetailsResponse"
Cohesion: 0.21
Nodes (5): LearningProgramDetailsResponse, LearningProgramModuleDetailsResponse, ProgramModuleResponse, ProgramSubjectResponse, StudentProgramDetailsResponse

### Community 139 - "ProgrammingLanguage"
Cohesion: 0.23
Nodes (6): CreateTaskRequest, ProgrammingTaskConfigRequest, TaskTestCaseRequest, ProgrammingTaskConfigInput, ProgrammingLanguage, PYTHON

### Community 140 - "TeacherStudentLinkId"
Cohesion: 0.22
Nodes (3): jakarta.persistence.Embeddable, Override, TeacherStudentLinkId

### Community 142 - ".createModule"
Cohesion: 0.25
Nodes (3): CreateLearningProgramModuleRequest, LearningProgramModuleResponse, UpdateLearningProgramModuleRequest

### Community 143 - "CurrentProgressResponse"
Cohesion: 0.31
Nodes (8): AssessmentResponse, CurrentProgressResponse, HomeworkResponse, PracticeResponse, TopicResponse, TopicsResponse, Override, Override

### Community 144 - ".getInvitation"
Cohesion: 0.27
Nodes (5): GetMapping, Override, PublicStudentInviteResponse, StudentName, TeacherName

### Community 145 - "JpaSubmissionQuery"
Cohesion: 0.24
Nodes (3): Override, JpaSubmissionQuery, MutableHomeworkItemState

### Community 146 - "DemoDataSeeder"
Cohesion: 0.33
Nodes (6): org.springframework.boot.ApplicationArguments, org.springframework.boot.ApplicationRunner, org.springframework.boot.autoconfigure.condition.ConditionalOnProperty, org.springframework.context.annotation.Profile, DemoDataSeeder, Override

### Community 147 - "ProgressReportNotEditableException"
Cohesion: 0.22
Nodes (3): InvalidProgressReportStateException, ProgressReportNotEditableException, ProgressReportNotPublishableException

### Community 148 - ".createInvite"
Cohesion: 0.28
Nodes (3): CreateStudentInviteRequest, StudentInviteCreatedResponse, PostMapping

### Community 150 - "SecurityProbeController"
Cohesion: 0.28
Nodes (6): GetMapping, PostMapping, RequestMapping, ResponseStatus, RestController, SecurityProbeController

### Community 151 - "ReportShareStatus"
Cohesion: 0.29
Nodes (5): ReportShareSummaryResponse, ReportShareStatus, ACTIVE, EXPIRED, REVOKED

### Community 152 - "StudentInviteStatus"
Cohesion: 0.29
Nodes (6): StudentInviteSummaryResponse, StudentInviteStatus, ACCEPTED, ACTIVE, EXPIRED, REVOKED

### Community 153 - "Core design decisions"
Cohesion: 0.29
Nodes (7): Code execution is isolated, Core design decisions, Historical reports are immutable snapshots, LearningProgram vs StudentProgram, Progress is derived from learning facts, Student can exist without a User account, Teacher ↔ Student ownership

### Community 154 - "Основные решения"
Cohesion: 0.29
Nodes (7): Current Progress не хранится отдельной таблицей, LearningProgram и StudentProgram — разные сущности, Ownership Teacher ↔ Student, Student существует независимо от User account, Выполнение кода изолировано, Исторические отчёты сохраняют snapshot, Основные решения

### Community 155 - ".listTasks"
Cohesion: 0.38
Nodes (3): TaskPageResponse, GetMapping, TaskPageResult

### Community 156 - "TraceIdFilter"
Cohesion: 0.47
Nodes (4): jakarta.servlet.FilterChain, org.springframework.web.filter.OncePerRequestFilter, Override, TraceIdFilter

### Community 157 - "File lesson materials (MVP)"
Cohesion: 0.40
Nodes (4): Compensation and operational limits, Configuration, File lesson materials (MVP), Teacher API

### Community 162 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

### Community 163 - "Technology stack"
Cohesion: 0.50
Nodes (4): Backend, Frontend, Infrastructure, Technology stack

### Community 164 - "Testing strategy"
Cohesion: 0.50
Nodes (4): Backend, Critical E2E scenarios, Frontend, Testing strategy

### Community 165 - "Технологии"
Cohesion: 0.50
Nodes (4): Backend, Frontend, Infrastructure, Технологии

### Community 166 - "Тестирование"
Cohesion: 0.50
Nodes (4): Backend, Frontend, Основные E2E сценарии, Тестирование

## Knowledge Gaps
- **169 isolated node(s):** `TEACHER`, `STUDENT`, `ADMIN`, `MARKDOWN`, `TEXT` (+164 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **38 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AuthenticatedUser` connect `AuthenticatedUser` to `.assign`, `AuthIntegrationTest`, `HomeworkStatus`, `StudentEntity`, `TaskQuery`, `PostgresIntegrationTest`, `StudentAccountStatus`, `HomeworkEntity`, `LearningProgramDetailsResponse`, `ProgressReport`, `org.springframework.transaction.annotation.Transactional`, `.createModule`, `CurrentProgressResponse`, `TeacherProgramManagementApiIntegrationTest`, `TeacherEntity`, `org.springframework.test.context.DynamicPropertyRegistry`, `ProgressShareApiIntegrationTest`, `.createInvite`, `ProgressReportPdfService`, `jakarta.servlet.http.HttpServletResponse`, `LessonMaterialEntity`, `SubmissionService`, `DemoDataSeedService.java`, `HomeworkApplicationIntegrationTest`, `.listTasks`, `ProgressShare`, `TeacherTaskController.java`, `io.swagger.v3.oas.annotations.media.Schema`, `TaskTestCase`, `TeacherLearningProgramService`, `org.junit.jupiter.api.Test`, `StudentInviteTokenService`, `SubmissionEntity`, `TeacherStudentProgramService`, `LessonMaterialApplicationIntegrationTest`, `SessionApiIntegrationTest`, `org.springframework.web.bind.annotation.GetMapping`, `SubmissionApiIntegrationTest`, `.run`, `TaskService`, `TeacherStudentInviteApiIntegrationTest`, `.createFixture`, `TeacherAssessmentService.java`, `HomeworkDetailsResponse`, `ProgressReportApplicationIntegrationTest`, `LessonMaterialResponse`, `AssessmentApiIntegrationTest`, `StudentInviteEntity`, `.user`, `AuthenticationSessionService`, `LessonMaterialService`, `.getId`, `LessonMaterialApiIntegrationTest`, `UserRepository`, `StudentHomeworkApiIntegrationTest`, `TeacherAssessmentService`, `TopicStatus`, `TeacherProgressReportController`, `TeacherLessonSessionController.java`, `ReportShareApiIntegrationTest`, `StudentSubmissionController`, `LessonSessionService`, `TeacherStudentApiIntegrationTest`, `TaskTestCaseRepository`, `TaskEntity`, `ProgressReportApiIntegrationTest`, `UpdateProgressReportRequest`, `.response`, `StudentInviteService`, `TeacherProgressShareController.java`, `ProgressInterval`?**
  _High betweenness centrality (0.177) - this node is a cross-community bridge._
- **Why does `HomeworkStatus` connect `HomeworkStatus` to `AuthenticatedUser`, `StudentEntity`, `HomeworkDetailsResponse`, `HomeworkDatabaseModel`, `TaskQuery`, `HomeworkCompletionServiceTest`, `HomeworkPersistenceIntegrationTest`, `HomeworkEntity`, `org.springframework.transaction.annotation.Transactional`, `TeacherEntity`, `SubmissionDatabaseModel`, `SubmissionApiIntegrationTest`, `TaskType`, `DemoDataSeedService.java`, `HomeworkApplicationIntegrationTest`, `StudentHomeworkApiIntegrationTest`, `HomeworkRepository`, `org.springframework.stereotype.Repository`?**
  _High betweenness centrality (0.042) - this node is a cross-community bridge._
- **Why does `ApiError` connect `AuthenticatedUser` to `org.springframework.http.ResponseEntity`, `.handleInvalidStudentListParameter`, `StudentSubmissionController`, `.handleInvalidSubmission`, `.handleInvalidTestCase`, `HomeworkDetailsResponse`, `ProgramExceptionHandler.java`, `.handleInvalidSessionListParameter`, `LessonMaterialResponse`, `org.springframework.web.bind.annotation.GetMapping`, `jakarta.servlet.http.HttpServletResponse`, `TeacherRegistrationInviteService`, `TeacherTaskController.java`, `TeacherProgressReportController`, `TeacherLessonSessionController.java`?**
  _High betweenness centrality (0.042) - this node is a cross-community bridge._
- **What connects `TEACHER`, `STUDENT`, `ADMIN` to the rest of the system?**
  _169 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `org.springframework.http.ResponseEntity` be split into smaller, more focused modules?**
  _Cohesion score 0.04393271272252164 - nodes in this community are weakly interconnected._
- **Should `AuthenticatedUser` be split into smaller, more focused modules?**
  _Cohesion score 0.052688953488372096 - nodes in this community are weakly interconnected._
- **Should `LearningPeriod` be split into smaller, more focused modules?**
  _Cohesion score 0.05347985347985348 - nodes in this community are weakly interconnected._