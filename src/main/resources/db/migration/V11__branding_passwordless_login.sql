ALTER TABLE app_branding_settings
    ADD COLUMN passwordless_login_enabled BOOLEAN NOT NULL DEFAULT FALSE;
