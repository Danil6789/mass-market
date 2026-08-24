-- user-service / user_db — seed an initial administrator.
--
-- Email:    admin@marketplace.local
-- Password: Admin123!
--
-- The BCrypt hash below was generated with BCryptPasswordEncoder(strength=10)
-- and verified locally with `matches("Admin123!", hash)` returning true.
-- Do NOT replace this hash with another one unless you also rotate the
-- shared credentials documented in .env.example.

INSERT INTO users (email, password, name, role, blocked, active)
VALUES (
        'admin@marketplace.local',
        '$2a$10$ZVj5t6aAR6jL4BJRu5U2R.vI2mpDzJKo7FtqkdYxNo9FgbJaXNvCa',
        'Administrator',
        'ADMIN',
        FALSE,
        TRUE
);