-- V2__athlete_profile_schema.sql
-- Creates the athlete profile tables: athlete_profiles, hr_profiles, hr_zones,
-- pace_profiles, and pace_zones in the trainer schema.

CREATE TABLE IF NOT EXISTS trainer.athlete_profiles (
    id                            BIGSERIAL    PRIMARY KEY,
    user_id                       BIGINT       NOT NULL UNIQUE,
    date_of_birth                 DATE         NOT NULL,
    weight_kg                     NUMERIC(4,1) NOT NULL,
    resting_hr                    INTEGER      NOT NULL,
    max_hr                        INTEGER      NOT NULL,
    lthr                          INTEGER,
    threshold_pace_seconds_per_km INTEGER,
    vo2_max                       NUMERIC(3,1),
    five_k_seconds                INTEGER,
    ten_k_seconds                 INTEGER,
    half_marathon_seconds         INTEGER,
    marathon_seconds              INTEGER,
    created_at                    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_athlete_profiles_user
        FOREIGN KEY (user_id) REFERENCES trainer.users(id)
);

CREATE TABLE IF NOT EXISTS trainer.hr_profiles (
    id                  BIGSERIAL   PRIMARY KEY,
    athlete_profile_id  BIGINT      NOT NULL UNIQUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_hr_profiles_athlete_profile
        FOREIGN KEY (athlete_profile_id) REFERENCES trainer.athlete_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS trainer.hr_zones (
    id             BIGSERIAL   PRIMARY KEY,
    hr_profile_id  BIGINT      NOT NULL,
    zone_number    INTEGER     NOT NULL,
    name           VARCHAR(50) NOT NULL,
    lower_bound    INTEGER     NOT NULL,
    upper_bound    INTEGER     NOT NULL,

    CONSTRAINT fk_hr_zones_hr_profile
        FOREIGN KEY (hr_profile_id) REFERENCES trainer.hr_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS trainer.pace_profiles (
    id                  BIGSERIAL   PRIMARY KEY,
    athlete_profile_id  BIGINT      NOT NULL UNIQUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_pace_profiles_athlete_profile
        FOREIGN KEY (athlete_profile_id) REFERENCES trainer.athlete_profiles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS trainer.pace_zones (
    id              BIGSERIAL   PRIMARY KEY,
    pace_profile_id BIGINT      NOT NULL,
    zone_number     INTEGER     NOT NULL,
    name            VARCHAR(50) NOT NULL,
    lower_bound     INTEGER     NOT NULL,
    upper_bound     INTEGER     NOT NULL,

    CONSTRAINT fk_pace_zones_pace_profile
        FOREIGN KEY (pace_profile_id) REFERENCES trainer.pace_profiles(id) ON DELETE CASCADE
);

-- Indexes for common lookups
CREATE INDEX idx_athlete_profiles_user_id ON trainer.athlete_profiles(user_id);
CREATE INDEX idx_hr_profiles_athlete_profile_id ON trainer.hr_profiles(athlete_profile_id);
CREATE INDEX idx_hr_zones_hr_profile_id ON trainer.hr_zones(hr_profile_id);
CREATE INDEX idx_pace_profiles_athlete_profile_id ON trainer.pace_profiles(athlete_profile_id);
CREATE INDEX idx_pace_zones_pace_profile_id ON trainer.pace_zones(pace_profile_id);
