package com.tutorplatform.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("demo & !prod")
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private final DemoDataSeedService seedService;

    public DemoDataSeeder(DemoDataSeedService seedService) {
        this.seedService = seedService;
    }

    @Override
    public void run(ApplicationArguments args) {
        DemoDataSeedService.SeedResult result = seedService.seed();
        log.info("DEMO DATA {}", result.created() ? "SEEDED" : "ALREADY PRESENT");
        log.info(
                "DEMO teacher: {} / {}",
                DemoDataAccess.TEACHER_EMAIL,
                DemoDataAccess.TEACHER_PASSWORD);
        log.info(
                "DEMO student Alex: {} / {}",
                DemoDataAccess.ALEX_EMAIL,
                DemoDataAccess.STUDENT_PASSWORD);
        log.info(
                "DEMO student Maria: {} / {}",
                DemoDataAccess.MARIA_EMAIL,
                DemoDataAccess.STUDENT_PASSWORD);
        log.info("DEMO public progress: /api/v1/public/progress/{}", DemoDataAccess.PROGRESS_TOKEN);
        log.info("DEMO public report: /api/v1/public/reports/{}", DemoDataAccess.REPORT_TOKEN);
    }
}
