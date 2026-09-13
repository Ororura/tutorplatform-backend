package com.tutorplatform.demo;

import com.tutorplatform.assessment.domain.TeacherAssessmentEntity;
import com.tutorplatform.assessment.domain.TeacherAssessmentRepository;
import com.tutorplatform.content.domain.LessonMaterialEntity;
import com.tutorplatform.content.domain.LessonMaterialRepository;
import com.tutorplatform.content.domain.LessonMaterialType;
import com.tutorplatform.homework.domain.HomeworkEntity;
import com.tutorplatform.homework.domain.HomeworkItemEntity;
import com.tutorplatform.homework.domain.HomeworkRepository;
import com.tutorplatform.homework.domain.HomeworkStatus;
import com.tutorplatform.program.domain.ModuleEntity;
import com.tutorplatform.program.domain.ModuleRepository;
import com.tutorplatform.program.domain.TopicEntity;
import com.tutorplatform.program.domain.TopicRepository;
import com.tutorplatform.program.domain.TopicStatus;
import com.tutorplatform.program.domain.learningprogram.LearningProgramEntity;
import com.tutorplatform.program.domain.learningprogram.LearningProgramRepository;
import com.tutorplatform.program.domain.learningprogram.LearningProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentProgramEntity;
import com.tutorplatform.program.domain.studentprogram.StudentProgramRepository;
import com.tutorplatform.program.domain.studentprogram.StudentProgramStatus;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressEntity;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressRepository;
import com.tutorplatform.program.domain.studentprogram.StudentTopicProgressStatus;
import com.tutorplatform.progress.application.GetCurrentProgressService;
import com.tutorplatform.progress.application.ProgressInterval;
import com.tutorplatform.progress.domain.ProgressShare;
import com.tutorplatform.progress.domain.ProgressShareRepository;
import com.tutorplatform.report.application.ProgressReportSnapshotV1Factory;
import com.tutorplatform.report.domain.LearningPeriod;
import com.tutorplatform.report.domain.LearningPeriodRepository;
import com.tutorplatform.report.domain.LearningPeriodStatus;
import com.tutorplatform.report.domain.ProgressReport;
import com.tutorplatform.report.domain.ProgressReportRepository;
import com.tutorplatform.report.domain.ProgressReportSnapshotSchemas;
import com.tutorplatform.report.domain.ProgressReportStatus;
import com.tutorplatform.report.domain.ReportShare;
import com.tutorplatform.report.domain.ReportShareRepository;
import com.tutorplatform.session.domain.AttendanceStatus;
import com.tutorplatform.session.domain.LessonSessionEntity;
import com.tutorplatform.session.domain.LessonSessionRepository;
import com.tutorplatform.session.domain.LessonSessionTopicEntity;
import com.tutorplatform.session.domain.LessonSessionTopicRepository;
import com.tutorplatform.student.application.invite.StudentInviteTokenService;
import com.tutorplatform.student.domain.StudentEntity;
import com.tutorplatform.student.domain.StudentStatus;
import com.tutorplatform.student.infrastructure.persistence.StudentInviteEntity;
import com.tutorplatform.student.infrastructure.persistence.StudentInviteRepository;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkEntity;
import com.tutorplatform.student.infrastructure.persistence.TeacherStudentLinkRepository;
import com.tutorplatform.subject.domain.SubjectRepository;
import com.tutorplatform.submission.domain.CodeExecutionStatus;
import com.tutorplatform.submission.domain.CodeSubmissionEntity;
import com.tutorplatform.submission.domain.CodeSubmissionRepository;
import com.tutorplatform.submission.domain.SubmissionEntity;
import com.tutorplatform.submission.domain.SubmissionRepository;
import com.tutorplatform.submission.domain.SubmissionStatus;
import com.tutorplatform.task.domain.programming.ComparisonMode;
import com.tutorplatform.task.domain.programming.ProgrammingLanguage;
import com.tutorplatform.task.domain.programming.ProgrammingTaskConfig;
import com.tutorplatform.task.domain.programming.ProgrammingTaskConfigRepository;
import com.tutorplatform.task.domain.programming.TaskTestCase;
import com.tutorplatform.task.domain.programming.TaskTestCaseRepository;
import com.tutorplatform.task.domain.task.TaskDifficulty;
import com.tutorplatform.task.domain.task.TaskEntity;
import com.tutorplatform.task.domain.task.TaskRepository;
import com.tutorplatform.task.domain.task.TaskStatus;
import com.tutorplatform.task.domain.task.TaskType;
import com.tutorplatform.task.domain.topic.TopicTaskEntity;
import com.tutorplatform.task.domain.topic.TopicTaskRepository;
import com.tutorplatform.user.domain.TeacherEntity;
import com.tutorplatform.user.domain.TeacherRepository;
import com.tutorplatform.user.domain.UserEntity;
import com.tutorplatform.user.domain.UserRepository;
import com.tutorplatform.user.domain.UserRole;
import com.tutorplatform.user.domain.UserStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.tutorplatform.demo.DemoDataIds.*;

@Service
@Profile("demo & !prod")
public class DemoDataSeedService {

    private static final int REPORT_INTERVAL_MINUTES = 300;

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final com.tutorplatform.student.domain.StudentRepository studentRepository;
    private final TeacherStudentLinkRepository teacherStudentLinkRepository;
    private final StudentInviteRepository studentInviteRepository;
    private final SubjectRepository subjectRepository;
    private final LearningProgramRepository learningProgramRepository;
    private final StudentProgramRepository studentProgramRepository;
    private final ModuleRepository moduleRepository;
    private final TopicRepository topicRepository;
    private final StudentTopicProgressRepository topicProgressRepository;
    private final LessonMaterialRepository materialRepository;
    private final LessonSessionRepository sessionRepository;
    private final LessonSessionTopicRepository sessionTopicRepository;
    private final TeacherAssessmentRepository assessmentRepository;
    private final TaskRepository taskRepository;
    private final ProgrammingTaskConfigRepository programmingConfigRepository;
    private final TaskTestCaseRepository testCaseRepository;
    private final TopicTaskRepository topicTaskRepository;
    private final HomeworkRepository homeworkRepository;
    private final SubmissionRepository submissionRepository;
    private final CodeSubmissionRepository codeSubmissionRepository;
    private final LearningPeriodRepository learningPeriodRepository;
    private final ProgressReportRepository progressReportRepository;
    private final ProgressShareRepository progressShareRepository;
    private final ReportShareRepository reportShareRepository;
    private final GetCurrentProgressService currentProgressService;
    private final ProgressReportSnapshotV1Factory snapshotFactory;
    private final PasswordEncoder passwordEncoder;
    private final StudentInviteTokenService tokenService;
    private final JdbcTemplate jdbcTemplate;

    @SuppressWarnings("java:S107")
    public DemoDataSeedService(
        UserRepository userRepository,
        TeacherRepository teacherRepository,
        com.tutorplatform.student.domain.StudentRepository studentRepository,
        TeacherStudentLinkRepository teacherStudentLinkRepository,
        StudentInviteRepository studentInviteRepository,
        SubjectRepository subjectRepository,
        LearningProgramRepository learningProgramRepository,
        StudentProgramRepository studentProgramRepository,
        ModuleRepository moduleRepository,
        TopicRepository topicRepository,
        StudentTopicProgressRepository topicProgressRepository,
        LessonMaterialRepository materialRepository,
        LessonSessionRepository sessionRepository,
        LessonSessionTopicRepository sessionTopicRepository,
        TeacherAssessmentRepository assessmentRepository,
        TaskRepository taskRepository,
        ProgrammingTaskConfigRepository programmingConfigRepository,
        TaskTestCaseRepository testCaseRepository,
        TopicTaskRepository topicTaskRepository,
        HomeworkRepository homeworkRepository,
        SubmissionRepository submissionRepository,
        CodeSubmissionRepository codeSubmissionRepository,
        LearningPeriodRepository learningPeriodRepository,
        ProgressReportRepository progressReportRepository,
        ProgressShareRepository progressShareRepository,
        ReportShareRepository reportShareRepository,
        GetCurrentProgressService currentProgressService,
        ProgressReportSnapshotV1Factory snapshotFactory,
        PasswordEncoder passwordEncoder,
        StudentInviteTokenService tokenService,
        JdbcTemplate jdbcTemplate
    ) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.teacherStudentLinkRepository = teacherStudentLinkRepository;
        this.studentInviteRepository = studentInviteRepository;
        this.subjectRepository = subjectRepository;
        this.learningProgramRepository = learningProgramRepository;
        this.studentProgramRepository = studentProgramRepository;
        this.moduleRepository = moduleRepository;
        this.topicRepository = topicRepository;
        this.topicProgressRepository = topicProgressRepository;
        this.materialRepository = materialRepository;
        this.sessionRepository = sessionRepository;
        this.sessionTopicRepository = sessionTopicRepository;
        this.assessmentRepository = assessmentRepository;
        this.taskRepository = taskRepository;
        this.programmingConfigRepository = programmingConfigRepository;
        this.testCaseRepository = testCaseRepository;
        this.topicTaskRepository = topicTaskRepository;
        this.homeworkRepository = homeworkRepository;
        this.submissionRepository = submissionRepository;
        this.codeSubmissionRepository = codeSubmissionRepository;
        this.learningPeriodRepository = learningPeriodRepository;
        this.progressReportRepository = progressReportRepository;
        this.progressShareRepository = progressShareRepository;
        this.reportShareRepository = reportShareRepository;
        this.currentProgressService = currentProgressService;
        this.snapshotFactory = snapshotFactory;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public SeedResult seed() {
        UserEntity existing = userRepository.findByEmail(DemoDataAccess.TEACHER_EMAIL).orElse(null);
        if (existing != null) {
            if (!existing.id().equals(TEACHER_USER)) {
                throw new IllegalStateException("Demo teacher email is already used by non-demo data");
            }
            SeedCounts counts = validateSeededDataset();
            return new SeedResult(false, counts);
        }

        Instant seedNow = Instant.now().truncatedTo(ChronoUnit.HOURS);
        SeedPeople people = seedPeople(seedNow);
        SeedPrograms programs = seedPrograms(people, seedNow);
        seedMaterials(programs, people.teacher());
        seedSessionsAndAssessments(programs, people.teacher(), seedNow);
        List<TaskEntity> tasks = seedTasks(programs, people.teacher());
        seedHomeworkAndSubmissions(programs, people, tasks, seedNow);
        seedPeriodsReportsAndShares(programs, people.teacher(), seedNow);
        return new SeedResult(true, validateSeededDataset());
    }

    private SeedPeople seedPeople(Instant seedNow) {
        UserEntity teacherUser = user(TEACHER_USER, DemoDataAccess.TEACHER_EMAIL,
            DemoDataAccess.TEACHER_PASSWORD, UserRole.TEACHER);
        TeacherEntity teacher = teacherRepository.saveAndFlush(new TeacherEntity(
            TEACHER, teacherUser, "Анна Смирнова"
        ));
        StudentEntity alex = registeredStudent(
            ALEX_USER, ALEX, DemoDataAccess.ALEX_EMAIL, "Алексей", "Иванов"
        );
        StudentEntity maria = registeredStudent(
            MARIA_USER, MARIA, DemoDataAccess.MARIA_EMAIL, "Мария", "Петрова"
        );
        StudentEntity ilya = studentRepository.saveAndFlush(new StudentEntity(
            ILYA, "Илья", "Соколов", StudentStatus.ACTIVE
        ));
        for (StudentEntity student : List.of(alex, maria, ilya)) {
            teacherStudentLinkRepository.saveAndFlush(new TeacherStudentLinkEntity(teacher, student));
        }
        studentInviteRepository.saveAndFlush(new StudentInviteEntity(
            ILYA_INVITE, ilya, teacher, "ilya.demo@tutor.local",
            tokenService.hash(DemoDataAccess.ILYA_INVITE_TOKEN), seedNow.plus(30, ChronoUnit.DAYS)
        ));
        return new SeedPeople(teacher, alex, maria, ilya);
    }

    private UserEntity user(UUID id, String email, String password, UserRole role) {
        UserEntity user = new UserEntity(id, email, passwordEncoder.encode(password), UserStatus.ACTIVE);
        user.addRole(role);
        return userRepository.saveAndFlush(user);
    }

    private StudentEntity registeredStudent(
        UUID userId,
        UUID studentId,
        String email,
        String firstName,
        String lastName
    ) {
        UserEntity user = user(userId, email, DemoDataAccess.STUDENT_PASSWORD, UserRole.STUDENT);
        StudentEntity student = new StudentEntity(studentId, firstName, lastName, StudentStatus.ACTIVE);
        student.linkUser(user.id());
        return studentRepository.saveAndFlush(student);
    }

    private SeedPrograms seedPrograms(SeedPeople people, Instant seedNow) {
        subjectRepository.findById(PYTHON_SUBJECT)
            .orElseThrow(() -> new IllegalStateException("System PYTHON subject from V009 is missing"));

        LearningProgramEntity alexTemplate = learningProgramRepository.saveAndFlush(
            new LearningProgramEntity(ALEX_LEARNING_PROGRAM, TEACHER, PYTHON_SUBJECT,
                "Python с нуля", "Базовая программа с практикой и домашними заданиями",
                LearningProgramStatus.ACTIVE)
        );
        StudentProgramEntity alexProgram = studentProgramRepository.saveAndFlush(
            new StudentProgramEntity(ALEX_PROGRAM, people.alex().getId(), alexTemplate.getId(), TEACHER,
                StudentProgramStatus.ACTIVE, REPORT_INTERVAL_MINUTES,
                seedNow.minus(42, ChronoUnit.DAYS), null)
        );
        String[] moduleTitles = {"Основы Python", "Управление программой", "Коллекции и функции"};
        String[][] topicTitles = {
            {"Введение в Python", "Переменные и типы данных", "Ввод и вывод"},
            {"Условия", "Циклы for", "Циклы while"},
            {"Списки", "Словари", "Функции"}
        };
        List<TopicEntity> alexTopics = new ArrayList<>();
        for (int moduleIndex = 0; moduleIndex < moduleTitles.length; moduleIndex++) {
            ModuleEntity module = moduleRepository.saveAndFlush(new ModuleEntity(
                ALEX_MODULES[moduleIndex], alexTemplate.getId(), moduleTitles[moduleIndex], null, moduleIndex
            ));
            for (int topicIndex = 0; topicIndex < 3; topicIndex++) {
                int flatIndex = moduleIndex * 3 + topicIndex;
                alexTopics.add(topicRepository.saveAndFlush(new TopicEntity(
                    ALEX_TOPICS[flatIndex], module.id(), topicTitles[moduleIndex][topicIndex], null,
                    topicIndex, TopicStatus.ACTIVE
                )));
            }
        }
        StudentTopicProgressStatus[] alexStatuses = {
            StudentTopicProgressStatus.COMPLETED, StudentTopicProgressStatus.COMPLETED,
            StudentTopicProgressStatus.COMPLETED, StudentTopicProgressStatus.COMPLETED,
            StudentTopicProgressStatus.COMPLETED, StudentTopicProgressStatus.IN_PROGRESS,
            StudentTopicProgressStatus.IN_PROGRESS, StudentTopicProgressStatus.AVAILABLE,
            StudentTopicProgressStatus.LOCKED
        };
        for (int index = 0; index < alexTopics.size(); index++) {
            StudentTopicProgressStatus status = alexStatuses[index];
            Instant startedAt = status == StudentTopicProgressStatus.LOCKED
                || status == StudentTopicProgressStatus.AVAILABLE ? null
                : seedNow.minus(35L - index * 3L, ChronoUnit.DAYS);
            Instant completedAt = status == StudentTopicProgressStatus.COMPLETED
                ? startedAt.plus(2, ChronoUnit.DAYS) : null;
            topicProgressRepository.saveAndFlush(new StudentTopicProgressEntity(
                alexProgram.id(), alexTopics.get(index).id(), status, startedAt, completedAt
            ));
        }

        LearningProgramEntity mariaTemplate = learningProgramRepository.saveAndFlush(
            new LearningProgramEntity(MARIA_LEARNING_PROGRAM, TEACHER, PYTHON_SUBJECT,
                "Python — начало обучения", "Короткая стартовая программа",
                LearningProgramStatus.ACTIVE)
        );
        StudentProgramEntity mariaProgram = studentProgramRepository.saveAndFlush(
            new StudentProgramEntity(MARIA_PROGRAM, people.maria().getId(), mariaTemplate.getId(), TEACHER,
                StudentProgramStatus.ACTIVE, REPORT_INTERVAL_MINUTES,
                seedNow.minus(12, ChronoUnit.DAYS), null)
        );
        ModuleEntity mariaModule = moduleRepository.saveAndFlush(new ModuleEntity(
            MARIA_MODULE, mariaTemplate.getId(), "Первые шаги", null, 0
        ));
        String[] mariaTitles = {"Знакомство с Python", "Переменные", "Условия"};
        StudentTopicProgressStatus[] mariaStatuses = {
            StudentTopicProgressStatus.COMPLETED,
            StudentTopicProgressStatus.IN_PROGRESS,
            StudentTopicProgressStatus.AVAILABLE
        };
        List<TopicEntity> mariaTopics = new ArrayList<>();
        for (int index = 0; index < mariaTitles.length; index++) {
            TopicEntity topic = topicRepository.saveAndFlush(new TopicEntity(
                MARIA_TOPICS[index], mariaModule.id(), mariaTitles[index], null, index, TopicStatus.ACTIVE
            ));
            mariaTopics.add(topic);
            Instant startedAt = index < 2 ? seedNow.minus(9L - index * 3L, ChronoUnit.DAYS) : null;
            Instant completedAt = index == 0 ? startedAt.plus(2, ChronoUnit.DAYS) : null;
            topicProgressRepository.saveAndFlush(new StudentTopicProgressEntity(
                mariaProgram.id(), topic.id(), mariaStatuses[index], startedAt, completedAt
            ));
        }
        return new SeedPrograms(alexProgram, mariaProgram, alexTopics, mariaTopics);
    }

    private void seedMaterials(SeedPrograms programs, TeacherEntity teacher) {
        record Material(int topic, LessonMaterialType type, String title, String content, String url) {}
        List<Material> materials = List.of(
            new Material(1, LessonMaterialType.MARKDOWN, "Краткий конспект: переменные",
                "# Переменные\nИмя связывается со значением с помощью оператора `=`.", null),
            new Material(4, LessonMaterialType.TEXT, "Основные правила работы с циклами",
                "Используйте for для обхода последовательностей и range для диапазонов.", null),
            new Material(0, LessonMaterialType.LINK, "Официальная документация Python",
                null, "https://docs.python.org/3/"),
            new Material(3, LessonMaterialType.CODE_EXAMPLE, "Пример условного оператора",
                "age = 18\nif age >= 18:\n    print(\"Доступ разрешён\")", null),
            new Material(6, LessonMaterialType.MARKDOWN, "Операции со списками",
                "- `append` добавляет элемент\n- `len` возвращает длину", null),
            new Material(8, LessonMaterialType.CODE_EXAMPLE, "Простая функция",
                "def greet(name):\n    return f\"Привет, {name}!\"", null)
        );
        for (int index = 0; index < materials.size(); index++) {
            Material value = materials.get(index);
            materialRepository.saveAndFlush(new LessonMaterialEntity(
                MATERIALS[index], programs.alexTopics().get(value.topic()).id(), teacher.id(), value.type(),
                value.title(), value.content(), null, value.url(), 0
            ));
        }
    }

    private void seedSessionsAndAssessments(
        SeedPrograms programs,
        TeacherEntity teacher,
        Instant seedNow
    ) {
        int[] daysAgo = {35, 31, 27, 24, 18, 14, 9, 5, 1};
        int[] minutes = {60, 90, 60, 60, 90, 60, 90, 60, 60};
        AttendanceStatus[] attendance = {
            AttendanceStatus.ATTENDED, AttendanceStatus.ATTENDED, AttendanceStatus.ATTENDED,
            AttendanceStatus.MISSED, AttendanceStatus.ATTENDED, AttendanceStatus.CANCELLED,
            AttendanceStatus.ATTENDED, AttendanceStatus.ATTENDED, AttendanceStatus.ATTENDED
        };
        String[] summaries = {
            "Познакомились с Python и запустили первую программу.",
            "Разобрали переменные, числа и строки.",
            "Практиковали ввод, вывод и преобразование типов.", null,
            "Условия if/elif/else и логические выражения.", null,
            "Цикл for, range и вложенные циклы.",
            "Цикл while и условия остановки.",
            "Начали работу со списками."
        };
        List<LessonSessionEntity> sessions = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            String privateNotes = index == 7 ? "DEMO PRIVATE: нужно повторить циклы" : null;
            LessonSessionEntity session = sessionRepository.saveAndFlush(new LessonSessionEntity(
                SESSIONS[index], programs.alexProgram().id(), teacher.id(),
                seedNow.minus(daysAgo[index], ChronoUnit.DAYS), minutes[index], attendance[index],
                summaries[index], privateNotes
            ));
            sessions.add(session);
            if (attendance[index] == AttendanceStatus.ATTENDED) {
                int topicIndex = Math.min(index, 6);
                sessionTopicRepository.saveAndFlush(new LessonSessionTopicEntity(
                    session.id(), programs.alexTopics().get(topicIndex).id(), true
                ));
            }
        }
        assessmentRepository.saveAndFlush(assessment(0, sessions.get(1), 3, 2, 3, 4,
            "Хорошо понял типы данных, стоит увереннее работать самостоятельно."));
        assessmentRepository.saveAndFlush(assessment(1, sessions.get(4), 4, 3, 4, 4,
            "Хорошо разобрался с условиями, нужно закрепить циклы."));
        assessmentRepository.saveAndFlush(assessment(2, sessions.get(6), 5, 4, 5, null,
            "Уверенно использует for и range."));
        assessmentRepository.saveAndFlush(assessment(3, sessions.get(8), 4, 4, 4, 5,
            "Хороший прогресс, можно переходить к словарям и функциям."));

        LessonSessionEntity mariaFirst = sessionRepository.saveAndFlush(new LessonSessionEntity(
            SESSIONS[9], programs.mariaProgram().id(), teacher.id(),
            seedNow.minus(8, ChronoUnit.DAYS), 60, AttendanceStatus.ATTENDED,
            "Установили Python и написали первую программу.", null
        ));
        sessionTopicRepository.saveAndFlush(new LessonSessionTopicEntity(
            mariaFirst.id(), programs.mariaTopics().get(0).id(), true
        ));
        sessionRepository.saveAndFlush(new LessonSessionEntity(
            SESSIONS[10], programs.mariaProgram().id(), teacher.id(),
            seedNow.minus(2, ChronoUnit.DAYS), 60, AttendanceStatus.MISSED,
            "Занятие не состоялось.", null
        ));
        assessmentRepository.saveAndFlush(assessment(4, mariaFirst, 4, 3, 3, null,
            "Хорошее начало, нужна регулярная практика."));
    }

    private TeacherAssessmentEntity assessment(
        int index, LessonSessionEntity session, Integer understanding, Integer independence,
        Integer practice, Integer homework, String comment
    ) {
        return new TeacherAssessmentEntity(
            ASSESSMENTS[index], session.id(), understanding, independence, practice, homework, comment
        );
    }

    private List<TaskEntity> seedTasks(SeedPrograms programs, TeacherEntity teacher) {
        String[] titles = {
            "Разница между = и ==", "Что делает условие if?",
            "Разница между list и tuple", "Когда использовать цикл while?",
            "Выведи Hello, World!", "Сумма двух чисел", "Проверка чётности",
            "Максимальное число в списке"
        };
        String[] descriptions = {
            "Объясни своими словами разницу между операторами `=` и `==`.",
            "Объясни назначение условного оператора `if` и приведи короткий пример.",
            "Сравни изменяемость и основные сценарии использования `list` и `tuple`.",
            "Когда цикл `while` удобнее цикла `for`? Приведи пример.",
            "Напиши программу, которая выводит строку `Hello, World!`.",
            "Прочитай два целых числа из одной строки и выведи их сумму.",
            "Прочитай целое число и выведи `YES`, если оно чётное, иначе `NO`.",
            "Прочитай целые числа из одной строки и выведи максимальное из них."
        };
        List<TaskEntity> result = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            TaskType type = index < 4 ? TaskType.TEXT : TaskType.CODE;
            TaskDifficulty difficulty = index < 5 ? TaskDifficulty.EASY : TaskDifficulty.MEDIUM;
            TaskEntity task = taskRepository.saveAndFlush(new TaskEntity(
                TASKS[index], teacher.id(), PYTHON_SUBJECT, titles[index], descriptions[index],
                type, difficulty, TaskStatus.ACTIVE
            ));
            result.add(task);
            topicTaskRepository.saveAndFlush(new TopicTaskEntity(
                programs.alexTopics().get(Math.min(index, 8)).id(), task.getId(), 0, true
            ));
        }
        seedProgrammingTask(4, "print(\"Hello, World!\")", List.of(
            test("", "Hello, World!\n", false), test("", "Hello, World!\n", true)));
        seedProgrammingTask(5, "a, b = map(int, input().split())", List.of(
            test("2 3\n", "5\n", false), test("-5 8\n", "3\n", true),
            test("100 250\n", "350\n", true)));
        seedProgrammingTask(6, "n = int(input())", List.of(
            test("4\n", "YES\n", false), test("7\n", "NO\n", false),
            test("0\n", "YES\n", true)));
        seedProgrammingTask(7, "numbers = list(map(int, input().split()))", List.of(
            test("1 9 3\n", "9\n", false), test("-8 -2 -10\n", "-2\n", true),
            test("42\n", "42\n", true)));
        return result;
    }

    private TestData test(String input, String output, boolean hidden) {
        return new TestData(input, output, hidden);
    }

    private void seedProgrammingTask(int taskIndex, String starterCode, List<TestData> tests) {
        UUID taskId = TASKS[taskIndex];
        programmingConfigRepository.saveAndFlush(new ProgrammingTaskConfig(
            taskId, ProgrammingLanguage.PYTHON, starterCode, true, 5_000, 128
        ));
        List<TaskTestCase> cases = new ArrayList<>();
        for (int index = 0; index < tests.size(); index++) {
            TestData test = tests.get(index);
            cases.add(new TaskTestCase(stableId("test-" + taskIndex + "-" + index), taskId,
                test.input(), test.output(), test.hidden(), ComparisonMode.NORMALIZED, index));
        }
        testCaseRepository.saveAllAndFlush(cases);
    }

    private void seedHomeworkAndSubmissions(
        SeedPrograms programs, SeedPeople people, List<TaskEntity> tasks, Instant seedNow
    ) {
        int item = 0;
        HomeworkEntity h1 = homework(0, programs.alexProgram(), "Основы Python", -30, -23,
            HomeworkStatus.COMPLETED, seedNow.minus(22, ChronoUnit.DAYS), seedNow,
            List.of(hwItem(item++, 0, true, tasks), hwItem(item++, 4, true, tasks),
                hwItem(item++, 2, false, tasks)));
        HomeworkEntity h2 = homework(1, programs.alexProgram(), "Условия", -20, -13,
            HomeworkStatus.COMPLETED, seedNow.minus(12, ChronoUnit.DAYS), seedNow,
            List.of(hwItem(item++, 1, true, tasks), hwItem(item++, 6, true, tasks)));
        HomeworkEntity h3 = homework(2, programs.alexProgram(), "Циклы", -6, 5,
            HomeworkStatus.ASSIGNED, null, seedNow,
            List.of(hwItem(item++, 3, true, tasks), hwItem(item++, 5, true, tasks),
                hwItem(item++, 7, false, tasks)));
        HomeworkEntity h4 = homework(3, programs.alexProgram(), "Практика со списками", -12, -2,
            HomeworkStatus.ASSIGNED, null, seedNow,
            List.of(hwItem(item++, 2, true, tasks), hwItem(item++, 7, true, tasks)));
        homework(4, programs.alexProgram(), "Повторение основ", -10, -3,
            HomeworkStatus.CANCELLED, null, seedNow,
            List.of(hwItem(item++, 0, true, tasks), hwItem(item++, 4, true, tasks)));
        HomeworkEntity mariaHomework = homework(5, programs.mariaProgram(), "Первое задание", -4, 3,
            HomeworkStatus.ASSIGNED, null, seedNow,
            List.of(hwItem(item++, 0, true, tasks), hwItem(item, 1, false, tasks)));

        int submission = 0;
        textSubmission(submission++, people.alex(), programs.alexProgram(), tasks.get(0), h1.getItems().get(0),
            1, SubmissionStatus.FAILED, "= присваивает, == тоже присваивает.", -29, seedNow);
        textSubmission(submission++, people.alex(), programs.alexProgram(), tasks.get(0), h1.getItems().get(0),
            2, SubmissionStatus.PASSED, "= присваивает значение, а == сравнивает два значения.", -28, seedNow);
        codeSubmission(submission++, people.alex(), programs.alexProgram(), tasks.get(4), h1.getItems().get(1),
            1, SubmissionStatus.PASSED, "print(\"Hello, World!\")", CodeExecutionStatus.PASSED,
            2, 2, 31, -27, seedNow);
        textSubmission(submission++, people.alex(), programs.alexProgram(), tasks.get(2), h1.getItems().get(2),
            1, SubmissionStatus.NEEDS_REVIEW,
            "Список изменяемый, кортеж неизменяемый; кортеж удобен для фиксированных данных.", -26, seedNow);
        textSubmission(submission++, people.alex(), programs.alexProgram(), tasks.get(1), h2.getItems().get(0),
            1, SubmissionStatus.PASSED, "if выполняет блок кода, когда условие истинно.", -18, seedNow);
        codeSubmission(submission++, people.alex(), programs.alexProgram(), tasks.get(6), h2.getItems().get(1),
            1, SubmissionStatus.PASSED,
            "n = int(input())\nprint(\"YES\" if n % 2 == 0 else \"NO\")",
            CodeExecutionStatus.PASSED, 3, 3, 38, -17, seedNow);
        textSubmission(submission++, people.alex(), programs.alexProgram(), tasks.get(3), h3.getItems().get(0),
            1, SubmissionStatus.NEEDS_REVIEW,
            "while нужен, когда заранее неизвестно количество повторов.", -4, seedNow);
        codeSubmission(submission++, people.alex(), programs.alexProgram(), tasks.get(5), h3.getItems().get(1),
            1, SubmissionStatus.FAILED, "a, b = map(int, input().split())\nprint(a - b)",
            CodeExecutionStatus.FAILED, 1, 3, 42, -4, seedNow);
        codeSubmission(submission++, people.alex(), programs.alexProgram(), tasks.get(5), h3.getItems().get(1),
            2, SubmissionStatus.PASSED, "a, b = map(int, input().split())\nprint(a + b)",
            CodeExecutionStatus.PASSED, 3, 3, 37, -3, seedNow);
        textSubmission(submission++, people.alex(), programs.alexProgram(), tasks.get(2), h4.getItems().get(0),
            1, SubmissionStatus.FAILED, "list и tuple одинаковые.", -7, seedNow);
        codeSubmission(submission++, people.alex(), programs.alexProgram(), tasks.get(7), h4.getItems().get(1),
            1, SubmissionStatus.FAILED, "while True:\n    pass", CodeExecutionStatus.TIMEOUT,
            0, 3, 5_000, -6, seedNow);
        textSubmission(submission++, people.alex(), programs.alexProgram(), tasks.get(0), null,
            1, SubmissionStatus.FAILED, "Оба оператора сравнивают.", -15, seedNow);
        textSubmission(submission++, people.alex(), programs.alexProgram(), tasks.get(0), null,
            2, SubmissionStatus.PASSED, "= связывает имя со значением, == проверяет равенство.", -14, seedNow);
        codeSubmission(submission++, people.alex(), programs.alexProgram(), tasks.get(6), null,
            1, SubmissionStatus.FAILED, "n = int(input())\nprint(unknown)",
            CodeExecutionStatus.RUNTIME_ERROR, 0, 3, 15, -8, seedNow);
        textSubmission(submission, people.maria(), programs.mariaProgram(), tasks.get(0),
            mariaHomework.getItems().get(0), 1, SubmissionStatus.NEEDS_REVIEW,
            "= записывает значение, == проверяет, одинаковые ли значения.", -1, seedNow);
    }

    private HomeworkEntity homework(
        int index, StudentProgramEntity program, String title, int assignedDays, int dueDays,
        HomeworkStatus status, Instant completedAt, Instant seedNow, List<HomeworkItemEntity> items
    ) {
        return homeworkRepository.saveAndFlush(new HomeworkEntity(
            HOMEWORK[index], program.id(), TEACHER, title, "Демонстрационное домашнее задание",
            seedNow.plus(assignedDays, ChronoUnit.DAYS), seedNow.plus(dueDays, ChronoUnit.DAYS),
            status, completedAt, items
        ));
    }

    private HomeworkItemEntity hwItem(int itemIndex, int taskIndex, boolean required, List<TaskEntity> tasks) {
        int homeworkIndex;
        int position;
        if (itemIndex < 3) { homeworkIndex = 0; position = itemIndex; }
        else if (itemIndex < 5) { homeworkIndex = 1; position = itemIndex - 3; }
        else if (itemIndex < 8) { homeworkIndex = 2; position = itemIndex - 5; }
        else if (itemIndex < 10) { homeworkIndex = 3; position = itemIndex - 8; }
        else if (itemIndex < 12) { homeworkIndex = 4; position = itemIndex - 10; }
        else { homeworkIndex = 5; position = itemIndex - 12; }
        return new HomeworkItemEntity(
            HOMEWORK_ITEMS[itemIndex], HOMEWORK[homeworkIndex], tasks.get(taskIndex).getId(), position, required
        );
    }

    private void textSubmission(
        int index, StudentEntity student, StudentProgramEntity program, TaskEntity task,
        HomeworkItemEntity item, int attempt, SubmissionStatus status, String answer,
        int days, Instant seedNow
    ) {
        submissionRepository.saveAndFlush(new SubmissionEntity(
            SUBMISSIONS[index], student.getId(), program.id(), task.getId(),
            item == null ? null : item.id(), attempt, status, answer,
            seedNow.plus(days, ChronoUnit.DAYS)
        ));
    }

    private void codeSubmission(
        int index, StudentEntity student, StudentProgramEntity program, TaskEntity task,
        HomeworkItemEntity item, int attempt, SubmissionStatus status, String source,
        CodeExecutionStatus executionStatus, int passed, int total, int timeMs,
        int days, Instant seedNow
    ) {
        textSubmission(index, student, program, task, item, attempt, status, null, days, seedNow);
        codeSubmissionRepository.saveAndFlush(new CodeSubmissionEntity(
            SUBMISSIONS[index], source, executionStatus, passed, total, timeMs,
            executionStatus == CodeExecutionStatus.PASSED ? "OK\n" : null,
            executionStatus == CodeExecutionStatus.RUNTIME_ERROR ? "NameError: name 'unknown' is not defined" : null,
            seedNow.plus(days, ChronoUnit.DAYS)
        ));
    }

    private void seedPeriodsReportsAndShares(
        SeedPrograms programs, TeacherEntity teacher, Instant seedNow
    ) {
        Instant periodStart = seedNow.minus(35, ChronoUnit.DAYS);
        Instant periodCompleted = seedNow.minus(18, ChronoUnit.DAYS);
        learningPeriodRepository.saveAndFlush(new LearningPeriod(
            ALEX_COMPLETED_PERIOD, programs.alexProgram().id(), 1, 0, REPORT_INTERVAL_MINUTES,
            300, LearningPeriodStatus.COMPLETED, periodStart, periodCompleted,
            periodStart, periodCompleted
        ));
        LearningPeriod alexActive = LearningPeriod.active(
            ALEX_ACTIVE_PERIOD, programs.alexProgram().id(), 2, 300,
            REPORT_INTERVAL_MINUTES, periodCompleted.plusSeconds(1)
        ).withStartedAt(seedNow.minus(9, ChronoUnit.DAYS), seedNow.minus(9, ChronoUnit.DAYS));
        learningPeriodRepository.saveAndFlush(alexActive);
        LearningPeriod mariaPeriod = LearningPeriod.active(
            MARIA_ACTIVE_PERIOD, programs.mariaProgram().id(), 1, 0,
            REPORT_INTERVAL_MINUTES, seedNow.minus(8, ChronoUnit.DAYS)
        ).withStartedAt(seedNow.minus(8, ChronoUnit.DAYS), seedNow.minus(8, ChronoUnit.DAYS));
        learningPeriodRepository.saveAndFlush(mariaPeriod);

        Instant reportCreated = periodCompleted.plus(1, ChronoUnit.DAYS);
        Instant publishedAt = reportCreated.plus(1, ChronoUnit.DAYS);
        ProgressReport report = progressReportRepository.saveAndFlush(new ProgressReport(
            PUBLISHED_REPORT, programs.alexProgram().id(), ALEX_COMPLETED_PERIOD, teacher.id(),
            ProgressReportStatus.PUBLISHED, periodStart, periodCompleted, 300,
            ProgressReportSnapshotSchemas.V1,
            snapshotFactory.create(currentProgressService.getProgressSnapshot(
                programs.alexProgram().id(), new ProgressInterval(periodStart, periodCompleted)
            ), 300),
            "За период Алексей уверенно освоил базовый синтаксис Python и условия. "
                + "Стоит продолжить практику с циклами.",
            "Закрепить циклы, перейти к спискам и функциям.",
            publishedAt, 0, reportCreated, publishedAt
        ));
        progressShareRepository.saveAndFlush(new ProgressShare(
            PROGRESS_SHARE, programs.alexProgram().id(), teacher.id(),
            tokenService.hash(DemoDataAccess.PROGRESS_TOKEN), null, null, seedNow
        ));
        reportShareRepository.saveAndFlush(new ReportShare(
            REPORT_SHARE, report.id(), teacher.id(), tokenService.hash(DemoDataAccess.REPORT_TOKEN),
            null, null, seedNow
        ));
        reportShareRepository.saveAndFlush(new ReportShare(
            EXPIRED_REPORT_SHARE, report.id(), teacher.id(),
            tokenService.hash(DemoDataAccess.EXPIRED_REPORT_TOKEN),
            seedNow.minus(1, ChronoUnit.DAYS), null, seedNow.minus(10, ChronoUnit.DAYS)
        ));
    }

    private SeedCounts validateSeededDataset() {
        SeedCounts counts = new SeedCounts(
            count("teachers"), count("students"), count("student_programs"), count("modules"),
            count("topics"), count("lesson_materials"), count("lesson_sessions"),
            count("teacher_assessments"), count("tasks"), count("homeworks"),
            count("submissions"), count("learning_periods"),
            countWhere("progress_reports", "status = 'PUBLISHED'"), count("progress_shares"),
            count("report_shares")
        );
        if (counts.teachers() < 1 || counts.students() < 3 || counts.studentPrograms() < 2
            || counts.modules() < 4 || counts.topics() < 12 || counts.materials() < 6
            || counts.sessions() < 11 || counts.assessments() < 5 || counts.tasks() < 8
            || counts.homework() < 6 || counts.submissions() < 15 || counts.learningPeriods() < 3
            || counts.publishedReports() < 1 || counts.progressShares() < 1 || counts.reportShares() < 2) {
            throw new IllegalStateException("Demo dataset is incomplete: " + counts);
        }
        requireFixedEntity("users", TEACHER_USER);
        requireFixedEntity("users", ALEX_USER);
        requireFixedEntity("users", MARIA_USER);
        for (UUID id : List.of(ALEX, MARIA, ILYA)) {
            requireFixedEntity("students", id);
        }
        for (UUID id : List.of(ALEX_PROGRAM, MARIA_PROGRAM)) {
            requireFixedEntity("student_programs", id);
        }
        for (UUID id : TASKS) {
            requireFixedEntity("tasks", id);
        }
        for (UUID id : HOMEWORK) {
            requireFixedEntity("homeworks", id);
        }
        requireFixedEntity("progress_reports", PUBLISHED_REPORT);
        requireFixedEntity("progress_shares", PROGRESS_SHARE);
        requireFixedEntity("report_shares", REPORT_SHARE);
        return counts;
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    private int countWhere(String table, String condition) {
        return jdbcTemplate.queryForObject(
            "select count(*) from " + table + " where " + condition, Integer.class
        );
    }

    private void requireFixedEntity(String table, UUID id) {
        Integer count = jdbcTemplate.queryForObject(
            "select count(*) from " + table + " where id = ?", Integer.class, id
        );
        if (count == null || count != 1) {
            throw new IllegalStateException("Missing stable demo entity " + table + "/" + id);
        }
    }

    private UUID stableId(String name) {
        return UUID.nameUUIDFromBytes(("tutor-demo-" + name).getBytes(StandardCharsets.UTF_8));
    }

    public record SeedResult(boolean created, SeedCounts counts) {
    }

    public record SeedCounts(
        int teachers, int students, int studentPrograms, int modules, int topics, int materials,
        int sessions, int assessments, int tasks, int homework, int submissions,
        int learningPeriods, int publishedReports, int progressShares, int reportShares
    ) {
    }

    private record SeedPeople(TeacherEntity teacher, StudentEntity alex, StudentEntity maria, StudentEntity ilya) {}
    private record SeedPrograms(
        StudentProgramEntity alexProgram, StudentProgramEntity mariaProgram,
        List<TopicEntity> alexTopics, List<TopicEntity> mariaTopics
    ) {}
    private record TestData(String input, String output, boolean hidden) {}
}
