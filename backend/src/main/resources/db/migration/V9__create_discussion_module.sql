CREATE TABLE discussion_questions (
    id UUID PRIMARY KEY,
    author_user_id UUID NOT NULL,
    title VARCHAR(180) NOT NULL,
    body TEXT NOT NULL,
    category VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    accepted_answer_id UUID,
    answer_count INTEGER NOT NULL DEFAULT 0,
    vote_score INTEGER NOT NULL DEFAULT 0,
    view_count INTEGER NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_discussion_questions_author
        FOREIGN KEY (author_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_discussion_questions_title
        CHECK (CHAR_LENGTH(BTRIM(title)) BETWEEN 5 AND 180),
    CONSTRAINT chk_discussion_questions_body
        CHECK (CHAR_LENGTH(BTRIM(body)) BETWEEN 10 AND 5000),
    CONSTRAINT chk_discussion_questions_category CHECK (
        category IN (
            'GENERAL',
            'ACADEMIC',
            'PROGRAMMING',
            'EXAMS',
            'CAREER',
            'CAMPUS',
            'OTHER'
        )
    ),
    CONSTRAINT chk_discussion_questions_status
        CHECK (status IN ('OPEN', 'RESOLVED', 'CLOSED', 'HIDDEN')),
    CONSTRAINT chk_discussion_questions_answer_count
        CHECK (answer_count >= 0),
    CONSTRAINT chk_discussion_questions_view_count
        CHECK (view_count >= 0),
    CONSTRAINT chk_discussion_questions_timestamps
        CHECK (updated_at >= created_at),
    CONSTRAINT chk_discussion_questions_version
        CHECK (version >= 0)
);

CREATE TABLE discussion_answers (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL,
    author_user_id UUID NOT NULL,
    body TEXT NOT NULL,
    accepted BOOLEAN NOT NULL DEFAULT FALSE,
    vote_score INTEGER NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_discussion_answers_question
        FOREIGN KEY (question_id)
        REFERENCES discussion_questions (id) ON DELETE CASCADE,
    CONSTRAINT fk_discussion_answers_author
        FOREIGN KEY (author_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT chk_discussion_answers_body
        CHECK (CHAR_LENGTH(BTRIM(body)) BETWEEN 10 AND 5000),
    CONSTRAINT chk_discussion_answers_accepted_deletion
        CHECK (NOT (accepted AND deleted)),
    CONSTRAINT chk_discussion_answers_timestamps
        CHECK (updated_at >= created_at),
    CONSTRAINT chk_discussion_answers_version
        CHECK (version >= 0)
);

ALTER TABLE discussion_questions
    ADD CONSTRAINT fk_discussion_questions_accepted_answer
    FOREIGN KEY (accepted_answer_id)
    REFERENCES discussion_answers (id) ON DELETE SET NULL;

CREATE TABLE discussion_question_votes (
    question_id UUID NOT NULL,
    user_id UUID NOT NULL,
    vote_value SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (question_id, user_id),
    CONSTRAINT fk_discussion_question_votes_question
        FOREIGN KEY (question_id)
        REFERENCES discussion_questions (id) ON DELETE CASCADE,
    CONSTRAINT fk_discussion_question_votes_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_discussion_question_votes_value
        CHECK (vote_value IN (-1, 1)),
    CONSTRAINT chk_discussion_question_votes_timestamps
        CHECK (updated_at >= created_at)
);

CREATE TABLE discussion_answer_votes (
    answer_id UUID NOT NULL,
    user_id UUID NOT NULL,
    vote_value SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (answer_id, user_id),
    CONSTRAINT fk_discussion_answer_votes_answer
        FOREIGN KEY (answer_id)
        REFERENCES discussion_answers (id) ON DELETE CASCADE,
    CONSTRAINT fk_discussion_answer_votes_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_discussion_answer_votes_value
        CHECK (vote_value IN (-1, 1)),
    CONSTRAINT chk_discussion_answer_votes_timestamps
        CHECK (updated_at >= created_at)
);

CREATE INDEX idx_discussion_questions_author_user_id
    ON discussion_questions (author_user_id);
CREATE INDEX idx_discussion_questions_category
    ON discussion_questions (category);
CREATE INDEX idx_discussion_questions_status
    ON discussion_questions (status);
CREATE INDEX idx_discussion_questions_created_at
    ON discussion_questions (created_at DESC);
CREATE INDEX idx_discussion_questions_vote_score
    ON discussion_questions (vote_score DESC);
CREATE INDEX idx_discussion_questions_answer_count
    ON discussion_questions (answer_count DESC);
CREATE INDEX idx_discussion_questions_deleted
    ON discussion_questions (deleted);
CREATE INDEX idx_discussion_questions_accepted_answer_id
    ON discussion_questions (accepted_answer_id)
    WHERE accepted_answer_id IS NOT NULL;

CREATE INDEX idx_discussion_answers_question_id
    ON discussion_answers (question_id);
CREATE INDEX idx_discussion_answers_author_user_id
    ON discussion_answers (author_user_id);
CREATE INDEX idx_discussion_answers_created_at
    ON discussion_answers (created_at);
CREATE UNIQUE INDEX uk_discussion_answers_one_accepted
    ON discussion_answers (question_id)
    WHERE accepted AND NOT deleted;

CREATE INDEX idx_discussion_question_votes_user_id
    ON discussion_question_votes (user_id);
CREATE INDEX idx_discussion_answer_votes_user_id
    ON discussion_answer_votes (user_id);
