-- V3__workout_schema.sql
-- Creates the workout tables: workouts and workout_steps in the trainer schema.

CREATE TABLE IF NOT EXISTS trainer.workouts (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         BIGINT       NOT NULL,
    name            VARCHAR(50)  NOT NULL,
    sport_type      VARCHAR(20)  NOT NULL,
    sub_sport       VARCHAR(30),
    num_valid_steps INTEGER      NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_workouts_user
        FOREIGN KEY (user_id) REFERENCES trainer.users(id),
    CONSTRAINT chk_workouts_num_valid_steps
        CHECK (num_valid_steps > 0)
);

CREATE TABLE IF NOT EXISTS trainer.workout_steps (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workout_id       UUID         NOT NULL,
    step_order       INTEGER      NOT NULL,
    step_name        VARCHAR(50),
    intensity        VARCHAR(20)  NOT NULL,
    duration_type    VARCHAR(40)  NOT NULL,
    duration_value   INTEGER,
    target_type      VARCHAR(20)  NOT NULL,
    target_value_low  INTEGER,
    target_value_high INTEGER,
    notes            VARCHAR(255),

    CONSTRAINT fk_workout_steps_workout
        FOREIGN KEY (workout_id) REFERENCES trainer.workouts(id) ON DELETE CASCADE,
    CONSTRAINT chk_workout_steps_step_order
        CHECK (step_order >= 0),
    CONSTRAINT uq_workout_steps_workout_id_step_order
        UNIQUE (workout_id, step_order)
);

-- Indexes for common lookups
CREATE INDEX idx_workouts_user_id ON trainer.workouts(user_id);
CREATE INDEX idx_workout_steps_workout_id ON trainer.workout_steps(workout_id);
