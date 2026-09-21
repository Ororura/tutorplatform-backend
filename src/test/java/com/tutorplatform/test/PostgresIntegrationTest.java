package com.tutorplatform.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class PostgresIntegrationTest {

    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withCommand("postgres", "-c", "max_connections=500");

    private static final Object DATABASE_LOCK = new Object();

    static {
        POSTGRES.start();
    }

    public static void configurePostgres(
            DynamicPropertyRegistry registry, String database, String flywayTarget) {
        ensureDatabase(database);
        registry.add("spring.datasource.url", () -> jdbcUrlForDatabase(database));
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        if (flywayTarget != null) {
            registry.add("spring.flyway.target", () -> flywayTarget);
        }
    }

    public static String jdbcUrlForDatabase(String database) {
        return POSTGRES.getJdbcUrl().replaceFirst("/[^/]+$", "/" + database);
    }

    public static void ensureDatabase(String database) {
        if (!database.matches("[a-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid test database: " + database);
        }
        synchronized (DATABASE_LOCK) {
            try (Connection connection =
                            DriverManager.getConnection(
                                    POSTGRES.getJdbcUrl(),
                                    POSTGRES.getUsername(),
                                    POSTGRES.getPassword());
                    PreparedStatement query =
                            connection.prepareStatement(
                                    "select 1 from pg_database where datname = ?")) {
                query.setString(1, database);
                if (!query.executeQuery().next()) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute("create database " + database);
                    }
                }
            } catch (SQLException exception) {
                throw new IllegalStateException(
                        "Could not create test database " + database, exception);
            }
        }
    }
}
