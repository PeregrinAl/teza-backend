CREATE TABLE questions (
    id UUID PRIMARY KEY,
    title VARCHAR(300) NOT NULL CHECK (length(trim(title)) > 0),
    description VARCHAR(10000) NOT NULL DEFAULT '',
    question_type VARCHAR(20) NOT NULL CHECK (
        question_type IN ('EMPIRICAL', 'CAUSAL', 'NORMATIVE', 'PREDICTIVE', 'PRACTICAL')
    ),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX questions_created_at_id_idx ON questions (created_at DESC, id DESC);
