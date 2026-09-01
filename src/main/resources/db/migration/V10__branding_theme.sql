ALTER TABLE app_branding_settings
    ADD COLUMN theme VARCHAR(32) NOT NULL DEFAULT 'DEFAULT';

ALTER TABLE app_branding_settings
    ADD CONSTRAINT chk_app_branding_theme
        CHECK (theme IN ('DEFAULT', 'OLIFAN_GROUP'));
