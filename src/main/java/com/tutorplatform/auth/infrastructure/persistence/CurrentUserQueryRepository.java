package com.tutorplatform.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CurrentUserQueryRepository {

    private final JdbcClient jdbcClient;

    public CurrentUserQueryRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<String> findDisplayName(UUID userId) {
        return jdbcClient
                .sql(
                        """
                SELECT COALESCE(
                    teacher.display_name,
                    trim(concat_ws(' ', student.first_name, student.last_name))
                )
                FROM users users
                LEFT JOIN teachers teacher ON teacher.user_id = users.id
                LEFT JOIN students student ON student.user_id = users.id
                WHERE users.id = :userId
                """)
                .param("userId", userId)
                .query(String.class)
                .optional();
    }
}
