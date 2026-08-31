CREATE TABLE app_branding_settings (
    id BIGINT NOT NULL,
    app_name VARCHAR(120) NOT NULL,
    image_data OID,
    image_content_type VARCHAR(40),
    image_version BIGINT NOT NULL,
    CONSTRAINT pk_app_branding_settings PRIMARY KEY (id)
);

INSERT INTO app_branding_settings (id, app_name, image_version)
VALUES (1, 'Racebets', 0);
