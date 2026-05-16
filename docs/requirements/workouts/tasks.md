# Implementation Plan: workouts

## Overview

Implement the Workouts feature as a backend-only REST API under `/api/workouts`. This provides full CRUD for Garmin FIT-compatible Workout and WorkoutStep entities, including a rich validation layer (`WorkoutStepValidator`) that enforces FIT SDK constraints on duration types, target types, repeat steps, and nested repeat prevention. The feature uses a Flyway migration (V3) for schema creation, Spring Data JPA entities with UUID identifiers, and ownership isolation (users can only access their own workouts).

## Tasks

- [x] 1. Create Flyway migration and JPA entities
  - [x] 1.1 Create Flyway migration `V3__workout_schema.sql`
    - Create `trainer.workouts` table with columns: id (UUID PK), user_id (BIGINT FK → trainer.users), name (VARCHAR 50), sport_type (VARCHAR 20), sub_sport (VARCHAR 30 nullable), num_valid_steps (INTEGER CHECK > 0), created_at (TIMESTAMPTZ), updated_at (TIMESTAMPTZ)
    - Create `trainer.workout_steps` table with columns: id (UUID PK), workout_id (UUID FK → trainer.workouts ON DELETE CASCADE), step_order (INTEGER CHECK >= 0), step_name (VARCHAR 50 nullable), intensity (VARCHAR 20), duration_type (VARCHAR 40), duration_value (INTEGER nullable), target_type (VARCHAR 20), target_value_low (INTEGER nullable), target_value_high (INTEGER nullable), notes (VARCHAR 255 nullable)
    - Add UNIQUE constraint on (workout_id, step_order)
    - Create index `idx_workouts_user_id` on `trainer.workouts(user_id)`
    - Create index `idx_workout_steps_workout_id` on `trainer.workout_steps(workout_id)`
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.8, 6.9_

  - [x] 1.2 Create enumerations in `com.trainer.workout`
    - Create `SportType` enum: RUNNING, CYCLING, SWIMMING, OTHER
    - Create `SubSport` enum: GENERIC, TREADMILL, TRAIL, TRACK, OPEN_WATER, LAP_SWIMMING
    - Create `Intensity` enum: ACTIVE, REST, WARMUP, COOLDOWN, RECOVERY, INTERVAL
    - Create `DurationType` enum: TIME, DISTANCE, HR_LESS_THAN, HR_GREATER_THAN, CALORIES, OPEN, POWER_LESS_THAN, POWER_GREATER_THAN, REPEAT_UNTIL_STEPS_COMPLETE
    - Create `TargetType` enum: SPEED, HEART_RATE, CADENCE, POWER, OPEN
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 1.3 Create JPA entities in `com.trainer.workout`
    - Create `Workout` entity mapped to `trainer.workouts` with UUID id, userId, name, sportType, subSport, numValidSteps, steps (OneToMany cascade ALL, orphanRemoval), createdAt, updatedAt, @PrePersist/@PreUpdate lifecycle callbacks
    - Create `WorkoutStep` entity mapped to `trainer.workout_steps` with UUID id, workout (ManyToOne LAZY), stepOrder, stepName, intensity, durationType, durationValue, targetType, targetValueLow, targetValueHigh, notes
    - Add UNIQUE constraint annotation on (workout_id, step_order)
    - _Requirements: 6.1, 6.2, 6.6, 6.7_

  - [x] 1.4 Create repositories in `com.trainer.workout`
    - Create `WorkoutRepository` extending `JpaRepository<Workout, UUID>` with methods: `findByUserIdOrderByCreatedAtDesc`, `findByUserIdAndSportTypeOrderByCreatedAtDesc`, `findByIdAndUserId`
    - Create `WorkoutStepRepository` extending `JpaRepository<WorkoutStep, UUID>` with method: `deleteByWorkoutId`
    - _Requirements: 3.1, 3.2, 3.6_

- [x] 2. Implement validation layer
  - [x] 2.1 Implement `WorkoutStepValidator` in `com.trainer.workout`
    - Create Spring `@Component` with method `validate(List<WorkoutStepRequest> steps)` that throws `WorkoutValidationException`
    - Implement duration value range checks: TIME [1000, 86400000], DISTANCE [1, 100000000], CALORIES [1, 10000], OPEN accepts null/zero, HR_LESS_THAN/HR_GREATER_THAN [0, 100] ∪ [101, 350], POWER_LESS_THAN/POWER_GREATER_THAN [0, 1000] ∪ [1001, 2500]
    - Implement target value range checks: SPEED [1, 100000], HEART_RATE [0, 100] ∪ [101, 350], CADENCE [0, 255], POWER [0, 1000] ∪ [1001, 2500], OPEN accepts null
    - Enforce targetValueLow <= targetValueHigh for ranged targets
    - Validate repeat steps: durationValue must reference valid preceding step index (>= 0 and < current index), targetValue (repetitions) in [1, 100], intensity must be REST, targetType must be OPEN
    - Detect nested repeats: range between repeat's durationValue and its index must not contain another REPEAT_UNTIL_STEPS_COMPLETE step
    - Report errors with stepIndex and field name
    - _Requirements: 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10, 2.11, 2.12, 2.13, 2.14, 2.15, 2.16, 2.20, 2.21, 2.23_

  - [ ]* 2.2 Write property test for duration value range enforcement (`WorkoutStepValidatorPropertyTest`)
    - **Property 3: Duration value range enforcement**
    - **Validates: Requirements 2.4, 2.5, 2.6, 2.8, 2.9**
    - For each DurationType, generate durationValues outside the valid range and verify validation throws with correct stepIndex and field
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` arbitraries for each duration type_

  - [ ]* 2.3 Write property test for target value range and ordering enforcement (`WorkoutStepValidatorPropertyTest`)
    - **Property 4: Target value range and ordering enforcement**
    - **Validates: Requirements 2.12, 2.13, 2.14, 2.15**
    - For each TargetType, generate target value pairs outside valid range or with low > high and verify validation throws
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` arbitraries for each target type_

  - [ ]* 2.4 Write property test for repeat step constraint enforcement (`WorkoutStepValidatorPropertyTest`)
    - **Property 5: Repeat step constraint enforcement**
    - **Validates: Requirements 2.10, 2.11, 2.23**
    - Generate repeat steps with invalid index, out-of-range repetitions, wrong intensity, or wrong targetType and verify validation throws
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` arbitraries for invalid repeat configurations_

  - [ ]* 2.5 Write property test for nested repeat detection (`WorkoutStepValidatorPropertyTest`)
    - **Property 6: No nested repeats**
    - **Validates: Requirements 2.20**
    - Generate step lists containing nested repeat structures and verify validation throws indicating nested repeats not supported
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` arbitraries for nested repeat step lists_

- [x] 3. Checkpoint — Ensure validation tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement DTOs and service layer
  - [x] 4.1 Create request/response DTOs in `com.trainer.workout`
    - Create `CreateWorkoutRequest` record with Bean Validation: @NotBlank @Size(max=50) name, @NotNull sportType, optional subSport, @NotEmpty @Size(max=50) @Valid steps
    - Create `UpdateWorkoutRequest` record (same structure as CreateWorkoutRequest)
    - Create `WorkoutStepRequest` record with: optional @Size(max=50) stepName, @NotNull intensity, @NotNull durationType, nullable durationValue, @NotNull targetType, nullable targetValueLow, nullable targetValueHigh, optional @Size(max=255) notes
    - Create `WorkoutResponse` record with: id, name, sportType, subSport, numValidSteps, createdAt, updatedAt, steps list
    - Create `WorkoutSummaryResponse` record with: id, name, sportType, subSport, numValidSteps, createdAt, updatedAt (no steps)
    - Create `WorkoutStepResponse` record with: id, stepOrder, stepName, intensity, durationType, durationValue, targetType, targetValueLow, targetValueHigh, notes
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 2.17, 2.18, 3.1, 3.2_

  - [x] 4.2 Implement `WorkoutService` in `com.trainer.workout`
    - `createWorkout(Long userId, CreateWorkoutRequest)`: parse enums, validate steps via WorkoutStepValidator, persist Workout + steps, set numValidSteps, return WorkoutResponse (201)
    - `getWorkouts(Long userId, SportType sportType)`: return list of WorkoutSummaryResponse ordered by createdAt desc, optionally filtered by sportType
    - `getWorkout(Long userId, UUID workoutId)`: return full WorkoutResponse with steps, throw WorkoutNotFoundException if not found/not owned
    - `updateWorkout(Long userId, UUID workoutId, UpdateWorkoutRequest)`: find owned workout (404 if not), validate new steps, clear existing steps, replace with new steps, update metadata, return WorkoutResponse
    - `deleteWorkout(Long userId, UUID workoutId)`: find owned workout (404 if not), delete (cascade removes steps), return void
    - Assign stepOrder based on position in request array
    - _Requirements: 1.1, 1.9, 1.10, 2.19, 3.1, 3.2, 3.3, 3.6, 4.1, 4.2, 4.3, 4.5, 4.6, 5.1, 5.2, 5.3_

  - [ ]* 4.3 Write property test for workout data round-trip (`WorkoutServicePropertyTest`)
    - **Property 1: Workout data round-trip**
    - **Validates: Requirements 1.1, 1.7, 1.9, 3.2**
    - For any valid workout payload, creating and then retrieving returns matching name, sportType, subSport, numValidSteps, and all step fields
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` valid workout arbitraries; uses Mockito for repositories_

  - [ ]* 4.4 Write property test for step ordering invariant (`WorkoutServicePropertyTest`)
    - **Property 7: Step ordering invariant**
    - **Validates: Requirements 2.19**
    - For any successfully created workout, each step's stepOrder equals its zero-based position in the array
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` valid workout arbitraries_

  - [ ]* 4.5 Write property test for full replacement semantics on update (`WorkoutServicePropertyTest`)
    - **Property 10: Full replacement semantics on update**
    - **Validates: Requirements 4.1, 4.2**
    - For any existing workout with M steps, updating with N new steps results in exactly N steps matching submitted data with no remnants
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` arbitraries for original and replacement step lists_

- [x] 5. Implement controller and exception handling
  - [x] 5.1 Create custom exceptions in `com.trainer.workout`
    - Create `WorkoutNotFoundException` (maps to 404)
    - Create `WorkoutValidationException` carrying a list of step-level errors (stepIndex, field, message)
    - _Requirements: 3.3, 3.4, 4.4, 4.5, 5.2, 5.3_

  - [x] 5.2 Implement `WorkoutController` in `com.trainer.workout`
    - `POST /api/workouts` → @Valid CreateWorkoutRequest → 201 with WorkoutResponse
    - `GET /api/workouts` → optional @RequestParam sportType → 200 with List<WorkoutSummaryResponse>
    - `GET /api/workouts/{id}` → 200 with WorkoutResponse or 404
    - `PUT /api/workouts/{id}` → @Valid UpdateWorkoutRequest → 200 with WorkoutResponse or 400/404
    - `DELETE /api/workouts/{id}` → 204 or 404
    - Extract authenticated user via @AuthenticationPrincipal
    - Handle invalid UUID path variable (MethodArgumentTypeMismatchException → 400)
    - Handle invalid sportType query parameter → 400
    - _Requirements: 1.1, 1.11, 3.1, 3.2, 3.4, 3.5, 3.7, 4.1, 4.7, 4.8, 5.1, 5.4, 5.5_

  - [x] 5.3 Extend `GlobalExceptionHandler` for workout exceptions
    - Handle `WorkoutNotFoundException` → 404 with `{ "message": "Workout not found" }`
    - Handle `WorkoutValidationException` → 400 with step-level error details (stepIndex, field, message)
    - Handle `MethodArgumentTypeMismatchException` for UUID → 400 with `{ "message": "Invalid identifier format" }`
    - Handle invalid enum query parameter → 400 with `{ "message": "Invalid sport type", "field": "sportType" }`
    - _Requirements: 3.3, 3.4, 3.7, 4.4, 4.5, 4.8, 5.2, 5.3, 5.5_

  - [ ]* 5.4 Write property test for invalid workout-level field rejection (`WorkoutValidationPropertyTest`)
    - **Property 2: Invalid workout-level field rejection**
    - **Validates: Requirements 1.2, 1.3, 1.4, 1.5, 1.6, 1.8**
    - For any workout request with exactly one invalid workout-level field (blank/oversized name, invalid sportType, invalid subSport, empty/>50 steps), verify HTTP 400
    - _Uses jqwik `@Property(tries = 100)` with MockMvc; arbitraries for single-field-invalid payloads_

- [x] 6. Checkpoint — Ensure all backend tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 7. Write backend integration tests
  - [ ]* 7.1 Write Spring Boot integration tests (`WorkoutIntegrationTest`)
    - Full CRUD cycle: POST → GET (single) → GET (list) → PUT → DELETE against test database
    - Verify 201 on create with valid payload, 200 on get/update, 204 on delete
    - Verify 400 on invalid payloads (validation errors with field details)
    - Verify 401 without JWT token
    - Verify ownership isolation: user A cannot access user B's workouts (returns 404, not 403)
    - Verify cascade delete: no orphan steps after workout deletion
    - Verify sport type filter returns only matching workouts
    - Verify full replacement on PUT: old steps removed, new steps persisted
    - Verify invalid UUID format returns 400
    - _Requirements: 1.1, 1.11, 3.1, 3.2, 3.3, 3.5, 3.6, 3.7, 4.1, 4.2, 4.4, 5.1, 5.2, 6.3_

  - [ ]* 7.2 Write property test for workout list ordering (`WorkoutServicePropertyTest`)
    - **Property 8: Workout list ordering**
    - **Validates: Requirements 3.1**
    - For any user with multiple workouts, GET /api/workouts returns workouts ordered by createdAt descending
    - _Uses jqwik `@Property(tries = 100)` with multiple workout creation and list verification_

  - [ ]* 7.3 Write property test for sport type filter correctness (`WorkoutServicePropertyTest`)
    - **Property 9: Sport type filter correctness**
    - **Validates: Requirements 3.6**
    - For any user with workouts of mixed sport types, filtering by sportType=X returns only workouts with that sport type
    - _Uses jqwik `@Property(tries = 100)` with mixed sport type workout creation_

  - [ ]* 7.4 Write property test for delete removes workout and all steps (`WorkoutServicePropertyTest`)
    - **Property 11: Delete removes workout and all steps**
    - **Validates: Requirements 5.1, 6.3**
    - For any existing workout, after delete, GET returns 404 and no WorkoutStep records exist for that workout
    - _Uses jqwik `@Property(tries = 100)` with create-then-delete verification_

- [x] 8. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Backend PBT uses jqwik (already in `pom.xml`) with `@Property(tries = 100)`
- Checkpoints ensure incremental validation at the validation layer and full backend boundaries
- The Flyway migration is V3 since V1 (users) and V2 (athlete profiles) already exist
- `SecurityConfig` already requires authentication on all non-`/api/auth/**` endpoints, so `/api/workouts/**` is automatically protected
- The `WorkoutStepValidator` is a pure validation function — ideal target for property-based testing
- PUT uses full replacement semantics: existing steps are deleted and replaced entirely
- Ownership checks use `findByIdAndUserId` — returns 404 (not 403) to avoid leaking existence information

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3"] },
    { "id": 2, "tasks": ["1.4", "2.1"] },
    { "id": 3, "tasks": ["2.2", "2.3", "2.4", "2.5"] },
    { "id": 4, "tasks": ["4.1"] },
    { "id": 5, "tasks": ["4.2", "5.1"] },
    { "id": 6, "tasks": ["4.3", "4.4", "4.5", "5.2"] },
    { "id": 7, "tasks": ["5.3", "5.4"] },
    { "id": 8, "tasks": ["7.1", "7.2", "7.3", "7.4"] }
  ]
}
```
