package com.pixsom.racebets.app;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppModeAndWordCloudMigrationTest {

    @Test
    void migrationRunsOnH2AndDerivesModeBeforeReplacingLegacyColumns() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:word-cloud-migration;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE app_feature_settings (id BIGINT PRIMARY KEY, betting_enabled BOOLEAN NOT NULL, quiz_enabled BOOLEAN NOT NULL)");
        jdbc.execute("CREATE TABLE app_user (id BIGINT PRIMARY KEY)");
        jdbc.update("INSERT INTO app_feature_settings (id, betting_enabled, quiz_enabled) VALUES (1, TRUE, FALSE), (2, FALSE, TRUE)");

        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V6__app_mode_and_word_cloud.sql"))
                .execute(dataSource);
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V7__word_cloud_moderation.sql"))
                .execute(dataSource);

        assertThat(jdbc.queryForList("SELECT active_mode FROM app_feature_settings ORDER BY id", String.class))
                .containsExactly("BETTING", "QUIZ");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'APP_FEATURE_SETTINGS' AND column_name IN ('BETTING_ENABLED', 'QUIZ_ENABLED')",
                Integer.class
        )).isZero();

        jdbc.update("INSERT INTO word_cloud_questions (text, status, active_slot, created_at, updated_at) VALUES ('Question 1', 'OPEN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO word_cloud_questions (text, status, active_slot, created_at, updated_at) VALUES ('Question 2', 'OPEN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
        )).hasMessageContaining("UK_WORD_CLOUD_QUESTIONS_ACTIVE_SLOT");
        jdbc.update("INSERT INTO word_cloud_questions (text, status, active_slot, created_at, updated_at) VALUES ('Archive 1', 'CLOSED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO word_cloud_questions (text, status, active_slot, created_at, updated_at) VALUES ('Archive 2', 'CLOSED', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        jdbc.update("INSERT INTO app_user (id) VALUES (10)");
        Long questionId = jdbc.queryForObject(
                "SELECT id FROM word_cloud_questions WHERE text = 'Question 1'", Long.class);
        jdbc.update(
                "INSERT INTO word_cloud_responses (question_id, user_id, display_text, normalized_text, created_at, updated_at) VALUES (?, 10, 'Paris', 'paris', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                questionId
        );
        jdbc.update(
                "INSERT INTO word_cloud_moderated_words (question_id, normalized_text) VALUES (?, 'paris')",
                questionId
        );
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO word_cloud_moderated_words (question_id, normalized_text) VALUES (?, 'paris')",
                questionId
        )).hasMessageContaining("UK_WORD_CLOUD_MODERATED_WORDS_QUESTION_TEXT");

        jdbc.update("DELETE FROM app_user WHERE id = 10");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM word_cloud_responses", Integer.class)).isZero();
        jdbc.update("DELETE FROM word_cloud_questions WHERE id = ?", questionId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM word_cloud_moderated_words", Integer.class)).isZero();
    }
}
