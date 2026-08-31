ALTER TABLE app_branding_settings
    ADD COLUMN login_title VARCHAR(160) NOT NULL DEFAULT 'Vivez la course, simplement.';

ALTER TABLE app_branding_settings
    ADD COLUMN login_subtitle VARCHAR(300) NOT NULL DEFAULT 'Pariez en direct, suivez les résultats et retrouvez votre classement au même endroit.';
