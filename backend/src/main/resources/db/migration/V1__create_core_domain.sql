CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    account_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_EMAIL',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_account_status CHECK (
        account_status IN (
            'PENDING_EMAIL',
            'PENDING_CAMPUS_REVIEW',
            'ACTIVE',
            'SUSPENDED',
            'DEACTIVATED'
        )
    )
);

CREATE UNIQUE INDEX uk_users_email_lower ON users (LOWER(email));

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name),
    CONSTRAINT chk_roles_name CHECK (name IN ('STUDENT', 'MODERATOR', 'ADMIN'))
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT
);

CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);

CREATE TABLE universities (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    short_name VARCHAR(20) NOT NULL,
    city VARCHAR(100) NOT NULL,
    website VARCHAR(2048),
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX uk_universities_name_lower
    ON universities (LOWER(name));
CREATE UNIQUE INDEX uk_universities_short_name_lower
    ON universities (LOWER(short_name));

CREATE TABLE departments (
    id UUID PRIMARY KEY,
    university_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_departments_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE RESTRICT,
    CONSTRAINT uk_departments_id_university UNIQUE (id, university_id)
);

CREATE UNIQUE INDEX uk_departments_university_name_lower
    ON departments (university_id, LOWER(name));
CREATE UNIQUE INDEX uk_departments_university_code_lower
    ON departments (university_id, LOWER(code));

CREATE TABLE courses (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL,
    course_code VARCHAR(30) NOT NULL,
    title VARCHAR(160) NOT NULL,
    recommended_semester SMALLINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_courses_department
        FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE RESTRICT,
    CONSTRAINT chk_courses_recommended_semester CHECK (
        recommended_semester IS NULL
        OR recommended_semester BETWEEN 1 AND 8
    )
);

CREATE UNIQUE INDEX uk_courses_department_code_lower
    ON courses (department_id, LOWER(course_code));

CREATE TABLE student_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    university_id UUID NOT NULL,
    department_id UUID NOT NULL,
    full_name VARCHAR(80) NOT NULL,
    semester SMALLINT NOT NULL,
    bio VARCHAR(500),
    avatar_url VARCHAR(2048),
    total_xp BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_student_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_student_profiles_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_student_profiles_university
        FOREIGN KEY (university_id) REFERENCES universities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_profiles_department
        FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_profiles_department_university
        FOREIGN KEY (department_id, university_id)
        REFERENCES departments (id, university_id) ON DELETE RESTRICT,
    CONSTRAINT chk_student_profiles_semester CHECK (semester BETWEEN 1 AND 8),
    CONSTRAINT chk_student_profiles_total_xp CHECK (total_xp >= 0)
);

CREATE INDEX idx_student_profiles_university_id
    ON student_profiles (university_id);
CREATE INDEX idx_student_profiles_department_id
    ON student_profiles (department_id);

INSERT INTO roles (id, name)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'STUDENT'),
    ('00000000-0000-0000-0000-000000000002', 'MODERATOR'),
    ('00000000-0000-0000-0000-000000000003', 'ADMIN');
