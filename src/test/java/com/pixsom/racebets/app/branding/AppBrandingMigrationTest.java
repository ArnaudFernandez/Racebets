package com.pixsom.racebets.app.branding;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppBrandingMigrationTest {

    @Test
    void migrationAddsCurrentLoginCopyToExistingBranding() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:branding-login-copy;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE app_branding_settings (id BIGINT PRIMARY KEY, app_name VARCHAR(120) NOT NULL)");
        jdbc.update("INSERT INTO app_branding_settings (id, app_name) VALUES (1, 'Racebets')");

        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V9__branding_login_copy.sql"))
                .execute(dataSource);

        assertThat(jdbc.queryForObject(
                "SELECT login_title FROM app_branding_settings WHERE id = 1", String.class))
                .isEqualTo(AppBrandingService.DEFAULT_LOGIN_TITLE);
        assertThat(jdbc.queryForObject(
                "SELECT login_subtitle FROM app_branding_settings WHERE id = 1", String.class))
                .isEqualTo(AppBrandingService.DEFAULT_LOGIN_SUBTITLE);
    }

    @Test
    void migrationAddsConstrainedDefaultThemeToExistingBranding() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:branding-theme;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE app_branding_settings (id BIGINT PRIMARY KEY, app_name VARCHAR(120) NOT NULL)");
        jdbc.update("INSERT INTO app_branding_settings (id, app_name) VALUES (1, 'Racebets')");

        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V10__branding_theme.sql"))
                .execute(dataSource);

        assertThat(jdbc.queryForObject(
                "SELECT theme FROM app_branding_settings WHERE id = 1", String.class))
                .isEqualTo("DEFAULT");
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE app_branding_settings SET theme = 'UNKNOWN' WHERE id = 1"))
                .isInstanceOf(RuntimeException.class);
    }
}
