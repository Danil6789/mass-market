-- user-service / user_db — initial users table.
-- Created in Phase 3 of the MassMarket plan.

CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,           -- BCrypt hash
    name        VARCHAR(100),
    phone       VARCHAR(20),
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER'
                CHECK (role IN ('USER', 'ADMIN')),
    blocked     BOOLEAN      NOT NULL DEFAULT FALSE,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role  ON users(role);