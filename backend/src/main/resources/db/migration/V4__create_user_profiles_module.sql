ALTER TABLE student_profiles
    ADD COLUMN cover_image_url VARCHAR(2048),
    ADD COLUMN location VARCHAR(100),
    ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',
    ADD CONSTRAINT chk_student_profiles_visibility
        CHECK (visibility IN ('PUBLIC', 'PRIVATE'));

CREATE TABLE skills (
    id UUID PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    normalized_name VARCHAR(40) NOT NULL,
    CONSTRAINT uk_skills_normalized_name UNIQUE (normalized_name),
    CONSTRAINT chk_skills_name_length
        CHECK (CHAR_LENGTH(name) BETWEEN 2 AND 40),
    CONSTRAINT chk_skills_normalized_name_length
        CHECK (CHAR_LENGTH(normalized_name) BETWEEN 2 AND 40)
);

CREATE TABLE student_skills (
    student_profile_id UUID NOT NULL,
    skill_id UUID NOT NULL,
    CONSTRAINT pk_student_skills
        PRIMARY KEY (student_profile_id, skill_id),
    CONSTRAINT fk_student_skills_profile
        FOREIGN KEY (student_profile_id)
        REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_student_skills_skill
        FOREIGN KEY (skill_id)
        REFERENCES skills (id) ON DELETE RESTRICT
);

CREATE INDEX idx_student_skills_skill_id
    ON student_skills (skill_id);

CREATE TABLE user_preferences (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    theme VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    compact_mode BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_preferences_user UNIQUE (user_id),
    CONSTRAINT fk_user_preferences_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_preferences_theme
        CHECK (theme IN ('SYSTEM', 'LIGHT', 'DARK')),
    CONSTRAINT chk_user_preferences_language
        CHECK (language ~ '^[a-z]{2,3}(-[A-Z]{2})?$'),
    CONSTRAINT chk_user_preferences_version CHECK (version >= 0)
);
