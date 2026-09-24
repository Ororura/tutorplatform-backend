package com.tutorplatform.demo;

import java.util.UUID;

/** Stable identifiers used by the developer-only demo dataset. */
public final class DemoDataIds {

    public static final UUID PYTHON_SUBJECT = id("6513554d-dceb-5902-b6df-557c7a94b5c7");

    public static final UUID TEACHER_USER = id("d0000000-0000-4000-8000-000000000001");
    public static final UUID TEACHER = id("d0000000-0000-4000-8000-000000000002");
    public static final UUID ALEX_USER = id("d0000000-0000-4000-8000-000000000003");
    public static final UUID ALEX = id("d0000000-0000-4000-8000-000000000004");
    public static final UUID MARIA_USER = id("d0000000-0000-4000-8000-000000000005");
    public static final UUID MARIA = id("d0000000-0000-4000-8000-000000000006");
    public static final UUID ILYA = id("d0000000-0000-4000-8000-000000000007");
    public static final UUID ILYA_INVITE = id("d0000000-0000-4000-8000-000000000008");

    public static final UUID ALEX_LEARNING_PROGRAM = id("d1000000-0000-4000-8000-000000000001");
    public static final UUID ALEX_PROGRAM = id("d1000000-0000-4000-8000-000000000002");
    public static final UUID MARIA_LEARNING_PROGRAM = id("d1000000-0000-4000-8000-000000000003");
    public static final UUID MARIA_PROGRAM = id("d1000000-0000-4000-8000-000000000004");
    public static final UUID REPORT_LEARNING_PROGRAM = id("d1000000-0000-4000-8000-000000000005");
    public static final UUID REPORT_STUDENT_PROGRAM = id("d1000000-0000-4000-8000-000000000006");
    public static final UUID REPORT_MODULE = id("d2000000-0000-4000-8000-000000000005");
    public static final UUID[] REPORT_TOPICS = sequence("d3000000-0000-4000-8000-", 3, 13);
    public static final UUID[] REPORT_SESSIONS = sequence("d5000000-0000-4000-8000-", 5, 12);
    public static final UUID REPORT_COMPLETED_PERIOD = id("da000000-0000-4000-8000-000000000004");

    public static final UUID[] ALEX_MODULES = sequence("d2000000-0000-4000-8000-", 3);
    public static final UUID[] ALEX_TOPICS = sequence("d3000000-0000-4000-8000-", 9);
    public static final UUID MARIA_MODULE = id("d2000000-0000-4000-8000-000000000004");
    public static final UUID[] MARIA_TOPICS = sequence("d3000000-0000-4000-8000-", 3, 10);
    public static final UUID[] MATERIALS = sequence("d4000000-0000-4000-8000-", 6);
    public static final UUID[] SESSIONS = sequence("d5000000-0000-4000-8000-", 11);
    public static final UUID[] ASSESSMENTS = sequence("d6000000-0000-4000-8000-", 5);
    public static final UUID[] TASKS = sequence("d7000000-0000-4000-8000-", 8);
    public static final UUID[] HOMEWORK = sequence("d8000000-0000-4000-8000-", 6);
    public static final UUID[] HOMEWORK_ITEMS = sequence("d8100000-0000-4000-8000-", 14);
    public static final UUID[] SUBMISSIONS = sequence("d9000000-0000-4000-8000-", 16);
    public static final UUID ALEX_COMPLETED_PERIOD = id("da000000-0000-4000-8000-000000000001");
    public static final UUID ALEX_ACTIVE_PERIOD = id("da000000-0000-4000-8000-000000000002");
    public static final UUID MARIA_ACTIVE_PERIOD = id("da000000-0000-4000-8000-000000000003");
    public static final UUID PUBLISHED_REPORT = id("db000000-0000-4000-8000-000000000001");
    public static final UUID PROGRESS_SHARE = id("dc000000-0000-4000-8000-000000000001");
    public static final UUID REPORT_SHARE = id("dc000000-0000-4000-8000-000000000002");
    public static final UUID EXPIRED_REPORT_SHARE = id("dc000000-0000-4000-8000-000000000003");

    private DemoDataIds() {}

    private static UUID id(String value) {
        return UUID.fromString(value);
    }

    private static UUID[] sequence(String prefix, int count) {
        return sequence(prefix, count, 1);
    }

    private static UUID[] sequence(String prefix, int count, int first) {
        UUID[] result = new UUID[count];
        for (int index = 0; index < count; index++) {
            result[index] = id(prefix + "%012d".formatted(first + index));
        }
        return result;
    }
}
