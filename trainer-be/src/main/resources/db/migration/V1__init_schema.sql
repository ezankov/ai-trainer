-- V1__init_schema.sql
-- Initial schema setup.
-- Flyway is configured to manage the 'trainer' schema, so all objects
-- created here will live in that schema.

-- Example: a simple users table
CREATE TABLE IF NOT EXISTS trainer.users (
    id         BIGSERIAL    PRIMARY KEY,
    username   VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
