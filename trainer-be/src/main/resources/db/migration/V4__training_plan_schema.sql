-- V4__training_plan_schema.sql
-- Creates the training plan tables: training_plans and plan_workouts in the trainer schema.

CREATE TABLE IF NOT EXISTS trainer.training_plans (
    id                         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                    BIGINT       NOT NULL,
    event_name                 VARCHAR(100) NOT NULL,
    distance                   VARCHAR(20)  NOT NULL,
    duration                   VARCHAR(10)  NOT NULL,
    race_date                  DATE         NOT NULL,
    target_pace_seconds_per_km INTEGER      NOT NULL,
    ai_model                   VARCHAR(20)  NOT NULL,
    training_days              INTEGER[]    NOT NULL,
    long_run_day               INTEGER      NOT NULL,
    state                      VARCHAR(20)  NOT NULL DEFAULT 'NEW',
    created_at                 TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_training_plans_user
        FOREIGN KEY (user_id) REFERENCES trainer.users(id),
    CONSTRAINT chk_training_plans_long_run_day
        CHECK (long_run_day BETWEEN 1 AND 7)
);

CREATE TABLE IF NOT EXISTS trainer.plan_workouts (
    id               UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    training_plan_id UUID    NOT NULL,
    workout_id       UUID    NOT NULL,
    week_number      INTEGER NOT NULL,
    day_of_week      INTEGER NOT NULL,
    order_in_day     INTEGER NOT NULL,

    CONSTRAINT fk_plan_workouts_training_plan
        FOREIGN KEY (training_plan_id) REFERENCES trainer.training_plans(id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_workouts_workout
        FOREIGN KEY (workout_id) REFERENCES trainer.workouts(id) ON DELETE RESTRICT,
    CONSTRAINT chk_plan_workouts_week_number
        CHECK (week_number > 0),
    CONSTRAINT chk_plan_workouts_day_of_week
        CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT chk_plan_workouts_order_in_day
        CHECK (order_in_day > 0),
    CONSTRAINT uq_plan_workouts_plan_week_day_order
        UNIQUE (training_plan_id, week_number, day_of_week, order_in_day)
);

-- Indexes for common lookups
CREATE INDEX idx_training_plans_user_id ON trainer.training_plans(user_id);
CREATE INDEX idx_training_plans_state ON trainer.training_plans(state);
CREATE INDEX idx_plan_workouts_training_plan_id ON trainer.plan_workouts(training_plan_id);
