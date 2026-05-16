# Requirements Document

## Introduction

This feature introduces Garmin-compatible Workout entities into the AI Trainer backend. A Workout is the top-level container representing a structured training session (e.g., an interval run, a tempo run, a long easy run). Each Workout contains an ordered list of WorkoutSteps that define the structure of the session — including warm-up, active intervals, rest periods, cooldowns, and repeat blocks.

The data model closely mirrors the Garmin FIT SDK workout structure to ensure future FIT file export compatibility. At this stage, no UI is required. Workouts are created and managed programmatically by the AI training plan generator via a REST API. These entities form the foundation for a future running plan feature that will compose multiple workouts per week into a training schedule.

## Glossary

- **Workout**: The JPA entity representing a structured training session. Stored in the `trainer.workouts` table. Contains metadata (sport type, sub-sport, name) and an ordered collection of WorkoutSteps. Belongs to a User.
- **Workout_Step**: The JPA entity representing a single step within a Workout. Stored in the `trainer.workout_steps` table. Defines duration, target, intensity, and ordering within the parent Workout.
- **Workout_API**: The Spring Boot REST controller handling endpoints under `/api/workouts` for CRUD operations on Workout resources.
- **Workout_Service**: The backend service layer responsible for business logic related to creating, reading, updating, and deleting Workouts and their steps.
- **Sport_Type**: An enumeration of supported sport types aligned with the Garmin FIT SDK (e.g., RUNNING, CYCLING, SWIMMING, OTHER).
- **Sub_Sport**: An enumeration of sub-sport classifications (e.g., GENERIC, TREADMILL, TRAIL, TRACK, OPEN_WATER, LAP_SWIMMING).
- **Intensity**: An enumeration describing the effort level of a WorkoutStep: ACTIVE, REST, WARMUP, COOLDOWN, RECOVERY, INTERVAL.
- **Duration_Type**: An enumeration describing how a step's duration is measured: TIME, DISTANCE, HR_LESS_THAN, HR_GREATER_THAN, CALORIES, OPEN, POWER_LESS_THAN, POWER_GREATER_THAN, REPEAT_UNTIL_STEPS_COMPLETE.
- **Target_Type**: An enumeration describing what metric a step targets: SPEED, HEART_RATE, CADENCE, POWER, OPEN.
- **Repeat_Step**: A WorkoutStep with duration type REPEAT_UNTIL_STEPS_COMPLETE, where `durationValue` references the zero-based index of the step to loop back to, and `targetValue` specifies the number of repetitions.
- **HR_Value_Encoding**: Heart rate target encoding convention from the Garmin FIT SDK: values 0–100 represent percentage of max HR; absolute BPM values are stored as BPM + 100 offset (e.g., 150 BPM is stored as 250).
- **Power_Value_Encoding**: Power target encoding convention from the Garmin FIT SDK: values 0–1000 represent percentage of FTP; absolute watt values are stored as watts + 1000 offset (e.g., 200W is stored as 1200).
- **User**: A person with a record in the `trainer.users` table who owns zero or more Workouts.

---

## Requirements

### Requirement 1: Create Workout

**User Story:** As the AI training plan generator, I want to create a workout with its steps in a single request, so that I can programmatically build structured training sessions for athletes.

#### Acceptance Criteria

1. WHEN a POST request is sent to `/api/workouts` with a valid workout payload containing a name, sport type, and at least one workout step, THE Workout_API SHALL create a new Workout record associated with the authenticated User and return HTTP 201 with the created Workout including its generated identifier, all metadata fields (name, sportType, subSport, numValidSteps, createdAt, updatedAt), and the full ordered list of WorkoutSteps.
2. WHEN a POST request is sent to `/api/workouts` with a missing or blank `name` field, THE Workout_API SHALL return HTTP 400 with a validation error message identifying the missing field.
3. WHEN a POST request is sent to `/api/workouts` with a `name` longer than 50 characters, THE Workout_API SHALL return HTTP 400 with a validation error message indicating the name exceeds the maximum length.
4. WHEN a POST request is sent to `/api/workouts` with an invalid `sportType` value that does not match any defined Sport_Type enumeration value, THE Workout_API SHALL return HTTP 400 with a validation error message indicating the invalid sport type.
5. WHEN a POST request is sent to `/api/workouts` with an empty `steps` list, THE Workout_API SHALL return HTTP 400 with a validation error message indicating at least one step is required.
6. WHEN a POST request is sent to `/api/workouts` with a `steps` list containing more than 50 steps, THE Workout_API SHALL return HTTP 400 with a validation error message indicating the maximum number of steps has been exceeded.
7. THE Workout_API SHALL accept `subSport` as an optional field. WHEN omitted, THE Workout_Service SHALL store it as null (defaulting to GENERIC semantics).
8. WHEN a POST request is sent to `/api/workouts` with a `subSport` value that does not match any defined Sub_Sport enumeration value, THE Workout_API SHALL return HTTP 400 with a validation error message indicating the invalid sub-sport type.
9. THE Workout_Service SHALL automatically set the `numValidSteps` field on the Workout entity to the count of steps provided in the request.
10. THE Workout_API SHALL apply all WorkoutStep validation rules defined in Requirement 2 to each step in the request payload before persisting the Workout.
11. IF a POST request is sent to `/api/workouts` without a valid JWT token or with an expired token, THEN THE Workout_API SHALL return HTTP 401 with an error message indicating authentication is required.

---

### Requirement 2: Workout Step Validation

**User Story:** As the AI training plan generator, I want workout steps to be validated against Garmin FIT SDK constraints, so that generated workouts are always exportable to Garmin devices.

#### Acceptance Criteria

1. THE Workout_API SHALL validate that each WorkoutStep contains a valid `intensity` value matching the Intensity enumeration.
2. THE Workout_API SHALL validate that each WorkoutStep contains a valid `durationType` value matching the Duration_Type enumeration.
3. THE Workout_API SHALL validate that each WorkoutStep contains a valid `targetType` value matching the Target_Type enumeration.
4. WHEN a WorkoutStep has `durationType` of TIME, THE Workout_API SHALL validate that `durationValue` is a positive integer representing milliseconds, with a minimum of 1000 (1 second) and a maximum of 86400000 (24 hours).
5. WHEN a WorkoutStep has `durationType` of DISTANCE, THE Workout_API SHALL validate that `durationValue` is a positive integer representing metres (centimetres in FIT scale: value in metres × 100), with a maximum of 100000000 (1000 km).
6. WHEN a WorkoutStep has `durationType` of CALORIES, THE Workout_API SHALL validate that `durationValue` is a positive integer representing calories, with a maximum of 10000.
7. WHEN a WorkoutStep has `durationType` of OPEN, THE Workout_API SHALL accept `durationValue` as null or zero, indicating the step ends when the user manually presses lap.
8. WHEN a WorkoutStep has `durationType` of HR_LESS_THAN or HR_GREATER_THAN, THE Workout_API SHALL validate that `durationValue` follows the HR_Value_Encoding convention (0–100 for percentage of max HR, or BPM + 100 for absolute values, valid absolute range: 101–350).
9. WHEN a WorkoutStep has `durationType` of POWER_LESS_THAN or POWER_GREATER_THAN, THE Workout_API SHALL validate that `durationValue` follows the Power_Value_Encoding convention (0–1000 for percentage of FTP, or watts + 1000 for absolute values, valid absolute range: 1001–2500).
10. WHEN a WorkoutStep has `durationType` of REPEAT_UNTIL_STEPS_COMPLETE, THE Workout_API SHALL validate that `durationValue` is a zero-based index referencing a preceding step in the same Workout (value must be >= 0 and less than the current step's index).
11. WHEN a WorkoutStep has `durationType` of REPEAT_UNTIL_STEPS_COMPLETE, THE Workout_API SHALL validate that `targetValue` (number of repetitions) is a positive integer between 1 and 100.
12. WHEN a WorkoutStep has `targetType` of SPEED, THE Workout_API SHALL validate that `targetValueLow` and `targetValueHigh` are positive integers representing speed in mm/s (millimetres per second), with a maximum of 100000 (360 km/h), where `targetValueLow` is less than or equal to `targetValueHigh`.
13. WHEN a WorkoutStep has `targetType` of HEART_RATE, THE Workout_API SHALL validate that `targetValueLow` and `targetValueHigh` follow the HR_Value_Encoding convention and that `targetValueLow` is less than or equal to `targetValueHigh`.
14. WHEN a WorkoutStep has `targetType` of CADENCE, THE Workout_API SHALL validate that `targetValueLow` and `targetValueHigh` are positive integers representing steps per minute (range 0–255), where `targetValueLow` is less than or equal to `targetValueHigh`.
15. WHEN a WorkoutStep has `targetType` of POWER, THE Workout_API SHALL validate that `targetValueLow` and `targetValueHigh` follow the Power_Value_Encoding convention and that `targetValueLow` is less than or equal to `targetValueHigh`.
16. WHEN a WorkoutStep has `targetType` of OPEN, THE Workout_API SHALL accept `targetValueLow` and `targetValueHigh` as null, indicating no specific target.
17. THE Workout_API SHALL accept `stepName` as an optional string field on each WorkoutStep with a maximum length of 50 characters.
18. THE Workout_API SHALL accept `notes` as an optional string field on each WorkoutStep with a maximum length of 255 characters.
19. THE Workout_Service SHALL assign each WorkoutStep a zero-based `stepOrder` field based on its position in the request array.
20. WHEN a Repeat_Step references a `durationValue` that would create a nested repeat (the range between `durationValue` and the repeat step's index contains another repeat step), THE Workout_API SHALL return HTTP 400 with a validation error message indicating nested repeats are not supported.
21. IF any WorkoutStep fails validation against criteria 1–18 or criterion 20, THEN THE Workout_API SHALL return HTTP 400 with a validation error message identifying the zero-based step index and the field that failed validation.
22. THE Workout_API SHALL validate that a Workout contains no more than 50 WorkoutSteps. IF the steps list exceeds 50 entries, THEN THE Workout_API SHALL return HTTP 400 with a validation error message indicating the maximum step count has been exceeded.
23. WHEN a WorkoutStep has `durationType` of REPEAT_UNTIL_STEPS_COMPLETE, THE Workout_API SHALL validate that the step's `intensity` is set to REST and its `targetType` is set to OPEN.

---

### Requirement 3: Retrieve Workouts

**User Story:** As the AI training plan generator, I want to retrieve workouts for a user, so that I can reference existing workouts when building training plans.

#### Acceptance Criteria

1. WHEN a GET request is sent to `/api/workouts` by an authenticated User, THE Workout_API SHALL return HTTP 200 with a list of all Workouts belonging to that User, each including metadata fields (id, name, sportType, subSport, numValidSteps, createdAt, updatedAt) but excluding the steps array, ordered by `createdAt` descending (most recent first).
2. WHEN a GET request is sent to `/api/workouts/{id}` by an authenticated User who owns the Workout with the given identifier, THE Workout_API SHALL return HTTP 200 with the full Workout including all WorkoutSteps ordered by `stepOrder`.
3. WHEN a GET request is sent to `/api/workouts/{id}` by an authenticated User who does not own the Workout, THE Workout_API SHALL return HTTP 404 with an error message indicating the workout was not found.
4. WHEN a GET request is sent to `/api/workouts/{id}` with an identifier that does not exist or that is not a valid UUID format, THE Workout_API SHALL return HTTP 404 with an error message indicating the workout was not found.
5. IF a GET request is sent to `/api/workouts` or `/api/workouts/{id}` without a valid authentication token, THEN THE Workout_API SHALL return HTTP 401.
6. WHEN a GET request is sent to `/api/workouts` with a valid `sportType` query parameter matching a Sport_Type enumeration value, THE Workout_API SHALL return HTTP 200 with only the Workouts belonging to the authenticated User that match the specified sport type, ordered by `createdAt` descending.
7. IF a GET request is sent to `/api/workouts` with a `sportType` query parameter that does not match any defined Sport_Type enumeration value, THEN THE Workout_API SHALL return HTTP 400 with a validation error message indicating the sport type is invalid.

---

### Requirement 4: Update Workout

**User Story:** As the AI training plan generator, I want to update an existing workout and its steps, so that I can refine training sessions as an athlete's plan evolves.

#### Acceptance Criteria

1. WHEN a PUT request is sent to `/api/workouts/{id}` with a valid updated workout payload by an authenticated User who owns the Workout, THE Workout_API SHALL replace the entire Workout (metadata and all steps) and return HTTP 200 with the full updated Workout including its identifier, all metadata fields, and the full ordered list of WorkoutSteps.
2. THE Workout_API SHALL treat PUT as a full replacement: the existing steps SHALL be deleted and replaced with the steps provided in the request body.
3. THE Workout_API SHALL apply the same validation rules for PUT requests as defined in Requirement 1 (acceptance criteria 2–7) and Requirement 2 (all acceptance criteria).
4. WHEN a PUT request is sent to `/api/workouts/{id}` by an authenticated User who does not own the Workout, THE Workout_API SHALL return HTTP 404 with an error message indicating the workout was not found.
5. WHEN a PUT request is sent to `/api/workouts/{id}` with an identifier that does not exist, THE Workout_API SHALL return HTTP 404 with an error message indicating the workout was not found.
6. WHEN a successful update occurs, THE Workout_Service SHALL update the `updatedAt` timestamp on the Workout entity to the current server time.
7. IF a PUT request is sent to `/api/workouts/{id}` without a valid JWT token or with an expired token, THEN THE Workout_API SHALL return HTTP 401 with an error message indicating authentication is required.
8. WHEN a PUT request is sent to `/api/workouts/{id}` where `{id}` is not a valid UUID format, THE Workout_API SHALL return HTTP 400 with an error message indicating the identifier format is invalid.

---

### Requirement 5: Delete Workout

**User Story:** As the AI training plan generator, I want to delete a workout that is no longer needed, so that obsolete training sessions do not clutter the user's workout library.

#### Acceptance Criteria

1. WHEN a DELETE request is sent to `/api/workouts/{id}` by an authenticated User who owns the Workout, THE Workout_API SHALL delete the Workout and all associated WorkoutSteps and return HTTP 204 with no response body.
2. WHEN a DELETE request is sent to `/api/workouts/{id}` by an authenticated User who does not own the Workout, THE Workout_API SHALL return HTTP 404 with an error message indicating the workout was not found.
3. WHEN a DELETE request is sent to `/api/workouts/{id}` with an identifier that does not exist, THE Workout_API SHALL return HTTP 404 with an error message indicating the workout was not found.
4. IF a DELETE request is sent to `/api/workouts/{id}` without a valid authentication token, THEN THE Workout_API SHALL return HTTP 401 with no response body.
5. IF a DELETE request is sent to `/api/workouts/{id}` where `{id}` is not a valid UUID format, THEN THE Workout_API SHALL return HTTP 400 with an error message indicating the identifier format is invalid.

---

### Requirement 6: Workout Data Persistence

**User Story:** As a developer, I want workouts and steps to be persisted in PostgreSQL with proper schema and constraints, so that data integrity is maintained and future FIT export is feasible.

#### Acceptance Criteria

1. THE Workout_Service SHALL persist Workouts in the `trainer.workouts` table with columns: `id` (UUID, primary key), `user_id` (BIGINT, foreign key to `trainer.users.id`, NOT NULL), `name` (VARCHAR 50, NOT NULL), `sport_type` (VARCHAR 20, NOT NULL), `sub_sport` (VARCHAR 30, nullable), `num_valid_steps` (INTEGER, NOT NULL, CHECK > 0), `created_at` (TIMESTAMP, NOT NULL), `updated_at` (TIMESTAMP, NOT NULL).
2. THE Workout_Service SHALL persist WorkoutSteps in the `trainer.workout_steps` table with columns: `id` (UUID, primary key), `workout_id` (UUID, foreign key to `trainer.workouts.id`, NOT NULL), `step_order` (INTEGER, NOT NULL, CHECK >= 0), `step_name` (VARCHAR 50, nullable), `intensity` (VARCHAR 20, NOT NULL), `duration_type` (VARCHAR 40, NOT NULL), `duration_value` (INTEGER, nullable), `target_type` (VARCHAR 20, NOT NULL), `target_value_low` (INTEGER, nullable), `target_value_high` (INTEGER, nullable), `notes` (VARCHAR 255, nullable).
3. THE Workout_Service SHALL enforce a cascading delete: when a Workout is deleted, all associated WorkoutSteps SHALL be deleted automatically via ON DELETE CASCADE on the foreign key constraint.
4. THE Workout_Service SHALL enforce a unique constraint on (`workout_id`, `step_order`) to prevent duplicate step ordering within a single Workout.
5. THE Workout_Service SHALL create the database schema using a Flyway migration script following the project's `V{n}__{description}.sql` naming convention.
6. THE Workout_Service SHALL use UUID as the identifier type for both Workout and WorkoutStep entities.
7. THE Workout_Service SHALL automatically populate `created_at` on entity creation and `updated_at` on every entity modification using JPA lifecycle callbacks (`@PrePersist` and `@PreUpdate`).
8. THE Flyway migration SHALL create an index on `trainer.workouts.user_id` to optimise queries filtering workouts by user.
9. THE Flyway migration SHALL create an index on `trainer.workout_steps.workout_id` to optimise queries loading steps for a workout.
