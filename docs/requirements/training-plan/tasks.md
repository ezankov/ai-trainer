# Implementation Plan: training-plan

## Overview

Implement the Training Plans feature as a full-stack addition to the AI Trainer application. The backend provides a Spring Boot REST API under `/api/training-plans` for CRUD and state lifecycle management (NEW → ACTIVE → COMPLETED/TERMINATED), with a `PlanWorkout` join entity linking plans to workouts with week/day/order scheduling metadata. A single-active-plan constraint is enforced transactionally. The frontend adds an Angular Plans page with two tabs — Active Plan view (week cards) and Plan List view — plus a creation dialog. Database schema is managed via Flyway migration V4 with UUID identifiers.

## Tasks

- [ ] 1. Create Flyway migration and backend enumerations
  - [ ] 1.1 Create Flyway migration `V4__training_plan_schema.sql`
    - Create `trainer.training_plans` table with columns: id (UUID PK DEFAULT gen_random_uuid()), user_id (BIGINT FK → trainer.users(id) NOT NULL), event_name (VARCHAR(100) NOT NULL), distance (VARCHAR(20) NOT NULL), duration (VARCHAR(10) NOT NULL), race_date (DATE NOT NULL), target_pace_seconds_per_km (INTEGER NOT NULL), ai_model (VARCHAR(20) NOT NULL), training_days (INTEGER[] NOT NULL), long_run_day (INTEGER NOT NULL CHECK BETWEEN 1 AND 7), state (VARCHAR(20) NOT NULL DEFAULT 'NEW'), created_at (TIMESTAMPTZ NOT NULL DEFAULT NOW()), updated_at (TIMESTAMPTZ NOT NULL DEFAULT NOW())
    - Create `trainer.plan_workouts` table with columns: id (UUID PK DEFAULT gen_random_uuid()), training_plan_id (UUID FK → trainer.training_plans(id) ON DELETE CASCADE NOT NULL), workout_id (UUID FK → trainer.workouts(id) ON DELETE RESTRICT NOT NULL), week_number (INTEGER NOT NULL CHECK > 0), day_of_week (INTEGER NOT NULL CHECK BETWEEN 1 AND 7), order_in_day (INTEGER NOT NULL CHECK > 0)
    - Add UNIQUE constraint on (training_plan_id, week_number, day_of_week, order_in_day)
    - Create index `idx_training_plans_user_id` on `trainer.training_plans(user_id)`
    - Create index `idx_training_plans_state` on `trainer.training_plans(state)`
    - Create index `idx_plan_workouts_training_plan_id` on `trainer.plan_workouts(training_plan_id)`
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 7.8, 7.9_

  - [ ] 1.2 Create enumerations in `com.trainer.trainingplan`
    - Create `PlanState` enum: NEW, ACTIVE, COMPLETED, TERMINATED
    - Create `PlanDuration` enum: WEEKS_8, WEEKS_10, WEEKS_12 with `getWeeks()` helper method
    - Create `PlanDistance` enum: FIVE_K, TEN_K, HALF_MARATHON, MARATHON
    - Create `AiModel` enum: CHATGPT, CLAUDE, GEMINI, KIRO, DUMMY
    - _Requirements: 1.4, 1.5, 1.9_

  - [ ] 1.3 Create JPA entities in `com.trainer.trainingplan`
    - Create `TrainingPlan` entity mapped to `trainer.training_plans` with UUID id, userId (Long), eventName, distance (PlanDistance), duration (PlanDuration), raceDate (LocalDate), targetPaceSecondsPerKm (Integer), aiModel (AiModel), trainingDays (List<Integer>), longRunDay (Integer), state (PlanState), planWorkouts (OneToMany cascade ALL, orphanRemoval, ordered by weekNumber/dayOfWeek/orderInDay), createdAt (OffsetDateTime), updatedAt (OffsetDateTime), @PrePersist/@PreUpdate lifecycle callbacks
    - Create `PlanWorkout` entity mapped to `trainer.plan_workouts` with UUID id, trainingPlan (ManyToOne LAZY), workout (ManyToOne LAZY referencing existing Workout entity), weekNumber (Integer), dayOfWeek (Integer), orderInDay (Integer)
    - Add UNIQUE constraint annotation on (training_plan_id, week_number, day_of_week, order_in_day)
    - _Requirements: 7.1, 7.2, 7.8, 1.14_

  - [ ] 1.4 Create repositories in `com.trainer.trainingplan`
    - Create `TrainingPlanRepository` extending `JpaRepository<TrainingPlan, UUID>` with methods: `findByUserIdOrderByCreatedAtDesc(Long userId)`, `findByIdAndUserId(UUID id, Long userId)`, `findByUserIdAndState(Long userId, PlanState state)`
    - Create `PlanWorkoutRepository` extending `JpaRepository<PlanWorkout, UUID>` with method: `findByTrainingPlanIdOrderByWeekNumberAscDayOfWeekAscOrderInDayAsc(UUID trainingPlanId)`
    - _Requirements: 4.1, 4.2, 2.1_


- [ ] 2. Implement DTOs, validation, and service layer
  - [ ] 2.1 Create request/response DTOs in `com.trainer.trainingplan`
    - Create `CreateTrainingPlanRequest` record with Bean Validation: @NotBlank @Size(max=100) eventName, @NotNull distance (String, validated against PlanDistance enum), @NotNull duration (String, validated against PlanDuration enum), @NotNull @Future raceDate (LocalDate), @NotNull @Min(150) @Max(900) targetPaceSecondsPerKm (Integer), @NotNull aiModel (String, validated against AiModel enum), @NotEmpty trainingDays (List<Integer>, custom validator for values 1–7, no duplicates), @NotNull @Min(1) @Max(7) longRunDay (Integer, custom validator: must be in trainingDays)
    - Create `TrainingPlanResponse` record with: id, eventName, distance, duration, raceDate, targetPaceSecondsPerKm, aiModel, trainingDays, longRunDay, state, createdAt, updatedAt
    - Create `TrainingPlanSummaryResponse` record (same shape as TrainingPlanResponse, used for list endpoint)
    - Create `TrainingPlanDetailResponse` record extending summary with: weeks (List<PlanWeekResponse>)
    - Create `PlanWeekResponse` record with: weekNumber, workouts (List<PlanWorkoutEntryResponse>)
    - Create `PlanWorkoutEntryResponse` record with: dayOfWeek, orderInDay, workout (WorkoutSummaryResponse with id, name, sportType, subSport, numValidSteps)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.8, 1.9, 1.10, 1.11, 1.12, 4.1, 4.2, 4.5_

  - [ ] 2.2 Implement `TrainingPlanService` in `com.trainer.trainingplan`
    - `createPlan(Long userId, CreateTrainingPlanRequest)`: validate enums, persist plan with state NEW, return TrainingPlanResponse
    - `getPlans(Long userId)`: return all plans ordered by createdAt desc as List<TrainingPlanSummaryResponse>
    - `getPlan(Long userId, UUID planId)`: return plan with workouts grouped by week, throw TrainingPlanNotFoundException if not found/not owned
    - `getActivePlan(Long userId)`: return active plan with workouts grouped by week, throw TrainingPlanNotFoundException if none active
    - `activatePlan(Long userId, UUID planId)`: within @Transactional, terminate current active (if any), activate target plan (must be NEW or COMPLETED), throw InvalidStateTransitionException for TERMINATED or already ACTIVE
    - `completePlan(Long userId, UUID planId)`: transition ACTIVE → COMPLETED, throw InvalidStateTransitionException if not ACTIVE
    - `terminatePlan(Long userId, UUID planId)`: transition ACTIVE/NEW → TERMINATED, throw InvalidStateTransitionException if COMPLETED or already TERMINATED
    - `deletePlan(Long userId, UUID planId)`: delete plan if not ACTIVE (throw InvalidStateTransitionException if ACTIVE), cascade to PlanWorkouts
    - _Requirements: 1.1, 1.14, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 3.1, 3.2, 3.3, 3.4, 3.5, 4.1, 4.2, 4.5, 4.6, 5.1, 5.2, 5.6_

  - [ ]* 2.3 Write property test for training plan creation round-trip (`TrainingPlanServicePropertyTest`)
    - **Property 1: Training plan creation round-trip**
    - **Validates: Requirements 1.1, 1.14**
    - For any valid creation payload (eventName 1–100 non-blank chars, distance in PlanDistance, duration in PlanDuration, future raceDate, pace 150–900, aiModel in AiModel, trainingDays non-empty subset of 1–7 unique values, longRunDay in trainingDays), creating and retrieving returns matching fields with state NEW and non-null timestamps
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` valid plan arbitraries_

  - [ ]* 2.4 Write property test for invalid creation payload rejection (`TrainingPlanValidationPropertyTest`)
    - **Property 2: Invalid creation payload rejection**
    - **Validates: Requirements 1.2, 1.3, 1.4, 1.5, 1.6, 1.8, 1.9, 1.10, 1.11, 1.12**
    - For any creation request with exactly one invalid field (blank/long eventName, invalid enum, past date, pace outside 150–900, null field, empty trainingDays, trainingDays with duplicates or values outside 1–7, longRunDay outside 1–7 or not in trainingDays), verify HTTP 400
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` single-field-invalid arbitraries_

  - [ ]* 2.5 Write property test for single active plan invariant (`TrainingPlanStatePropertyTest`)
    - **Property 3: Single active plan invariant**
    - **Validates: Requirements 2.1, 2.2, 2.3**
    - For any sequence of plan creations and activation requests, after each successful activation verify exactly one ACTIVE plan exists and all previously active plans are TERMINATED
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` arbitraries for plan creation/activation sequences_

  - [ ]* 2.6 Write property test for state machine transition correctness (`TrainingPlanStatePropertyTest`)
    - **Property 4: State machine transition correctness**
    - **Validates: Requirements 3.2, 3.3, 3.4, 3.5**
    - For any plan in a given state, verify valid transitions succeed with correct new state and invalid transitions throw InvalidStateTransitionException
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` arbitraries for state/transition combinations_

- [ ] 3. Checkpoint — Ensure service layer and property tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. Implement controller and exception handling
  - [ ] 4.1 Create custom exceptions in `com.trainer.trainingplan`
    - Create `TrainingPlanNotFoundException` (maps to 404)
    - Create `InvalidStateTransitionException` (maps to 400)
    - Create `PlanSchedulingException` (maps to 400)
    - _Requirements: 3.6, 3.8, 4.3, 4.4, 5.2, 5.3, 5.4_

  - [ ] 4.2 Implement `TrainingPlanController` in `com.trainer.trainingplan`
    - `POST /api/training-plans` → @Valid CreateTrainingPlanRequest → 201 with TrainingPlanResponse
    - `GET /api/training-plans` → 200 with List<TrainingPlanSummaryResponse>
    - `GET /api/training-plans/{id}` → 200 with TrainingPlanDetailResponse or 404
    - `GET /api/training-plans/active` → 200 with TrainingPlanDetailResponse or 404
    - `PUT /api/training-plans/{id}/activate` → 200 with TrainingPlanResponse or 400/404
    - `PUT /api/training-plans/{id}/complete` → 200 with TrainingPlanResponse or 400/404
    - `PUT /api/training-plans/{id}/terminate` → 200 with TrainingPlanResponse or 400/404
    - `DELETE /api/training-plans/{id}` → 204 or 400/404
    - Extract authenticated user via @AuthenticationPrincipal
    - Handle invalid UUID path variable → 404 (same as not found)
    - _Requirements: 1.1, 1.13, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ] 4.3 Extend `GlobalExceptionHandler` for training plan exceptions
    - Handle `TrainingPlanNotFoundException` → 404 with `{ "message": "Training plan not found" }`
    - Handle `InvalidStateTransitionException` → 400 with appropriate message (already active, cannot reactivate terminated, only active can be completed, cannot terminate from current state, must terminate before deletion)
    - Handle `PlanSchedulingException` → 400 with scheduling error details
    - Handle invalid UUID path variable for training plan endpoints → 404
    - _Requirements: 2.4, 2.5, 3.3, 3.5, 3.6, 3.8, 4.3, 4.4, 5.2, 5.3, 5.4_

  - [ ]* 4.4 Write property test for plan list ordering (`TrainingPlanServicePropertyTest`)
    - **Property 5: Plan list ordering invariant**
    - **Validates: Requirements 4.1**
    - For any user with N plans (N ≥ 2), GET /api/training-plans returns all N plans ordered by createdAt descending
    - _Uses jqwik `@Property(tries = 100)` with multiple plan creation and list verification_

  - [ ]* 4.5 Write property test for plan detail workout grouping (`TrainingPlanServicePropertyTest`)
    - **Property 6: Plan detail retrieval with correct workout grouping and ordering**
    - **Validates: Requirements 4.2, 6.2, 6.6**
    - For any plan with M PlanWorkout assignments across W weeks, verify response groups workouts by weekNumber ascending, and within each week orders by dayOfWeek ascending then orderInDay ascending
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` arbitraries for random PlanWorkout assignments_

- [ ] 5. Implement workout scheduling validation
  - [ ] 5.1 Add workout scheduling logic to `TrainingPlanService`
    - Validate weekNumber is in [1, plan.duration.getWeeks()]
    - Validate dayOfWeek is in [1, 7]
    - Validate orderInDay is in [1, 5]
    - Validate (weekNumber, dayOfWeek, orderInDay) uniqueness within plan
    - Validate plan state is NEW before allowing workout assignment
    - Validate referenced workout exists
    - Throw PlanSchedulingException with descriptive messages for violations
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.7, 6.8_

  - [ ]* 5.2 Write property test for workout scheduling validation (`TrainingPlanSchedulingPropertyTest`)
    - **Property 8: Workout scheduling validation**
    - **Validates: Requirements 6.1, 6.3, 6.4, 6.7**
    - For each PlanDuration, generate valid and invalid (weekNumber, dayOfWeek, orderInDay) tuples; verify acceptance/rejection matches constraints
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` arbitraries for scheduling tuples_

  - [ ]* 5.3 Write property test for delete preserves workouts (`TrainingPlanServicePropertyTest`)
    - **Property 7: Delete removes plan and associations but preserves workouts**
    - **Validates: Requirements 5.1, 5.6**
    - For any plan in state NEW/COMPLETED/TERMINATED with PlanWorkout records, after delete verify plan returns 404, no PlanWorkout records exist, and all referenced Workouts still exist
    - _Uses jqwik `@Property(tries = 100)` with create-then-delete verification_

- [ ] 6. Checkpoint — Ensure all backend tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 7. Write backend integration tests
  - [ ]* 7.1 Write Spring Boot integration tests (`TrainingPlanIntegrationTest`)
    - Full CRUD cycle: POST → GET (single) → GET (list) → GET (active) → PUT activate → PUT complete → PUT terminate → DELETE against test database
    - Verify 201 on create with valid payload, 200 on get/state transitions, 204 on delete
    - Verify 400 on invalid payloads (validation errors with field details)
    - Verify 400 on invalid state transitions (already active, cannot reactivate terminated, only active can complete, cannot terminate from completed/terminated)
    - Verify 400 on delete of active plan
    - Verify 401 without JWT token
    - Verify ownership isolation: user A cannot access user B's plans (returns 404, not 403)
    - Verify cascade delete: PlanWorkouts removed, Workouts preserved
    - Verify transactional activation: terminate + activate is atomic
    - Verify single active plan constraint after activation
    - Verify Flyway migration V4 runs successfully
    - Verify invalid UUID format returns 404
    - _Requirements: 1.1, 1.13, 2.1, 2.2, 2.6, 2.7, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 7.3, 7.4_

- [ ] 8. Implement frontend TypeScript interfaces and service
  - [ ] 8.1 Create TypeScript interfaces in `src/app/core/training-plan/`
    - Create `training-plan.models.ts` with interfaces: TrainingPlanSummary, TrainingPlanDetail, PlanWeek, PlanWorkoutEntry, WorkoutSummary, CreatePlanRequest
    - Create type aliases: PlanState, PlanDistance, PlanDuration, AiModel
    - _Requirements: 4.1, 4.2, 4.5, 11.2_

  - [ ] 8.2 Create `PaceFormatUtils` in `src/app/core/training-plan/pace-format.utils.ts`
    - Implement `secondsToPace(seconds: number): string` — converts seconds per km to MM:SS/km display string (e.g., 300 → "5:00")
    - Implement `paceToSeconds(pace: string): number | null` — converts MM:SS/km string to seconds per km (e.g., "5:00" → 300), returns null for invalid input
    - Implement `isValidPace(pace: string): boolean` — validates MM:SS/km string is within 150–900 seconds range
    - _Requirements: 11.2 (pace input behaviour)_

  - [ ] 8.3 Create `TrainingPlanService` in `src/app/core/training-plan/training-plan.service.ts`
    - Implement `getPlans()`: GET `/api/training-plans` → Observable<TrainingPlanSummary[]>
    - Implement `getPlan(id: string)`: GET `/api/training-plans/{id}` → Observable<TrainingPlanDetail>
    - Implement `getActivePlan()`: GET `/api/training-plans/active` → Observable<TrainingPlanDetail>
    - Implement `createPlan(request: CreatePlanRequest)`: POST `/api/training-plans` → Observable<TrainingPlanSummary>
    - Implement `activatePlan(id: string)`: PUT `/api/training-plans/{id}/activate` → Observable<TrainingPlanSummary>
    - Implement `completePlan(id: string)`: PUT `/api/training-plans/{id}/complete` → Observable<TrainingPlanSummary>
    - Implement `terminatePlan(id: string)`: PUT `/api/training-plans/{id}/terminate` → Observable<TrainingPlanSummary>
    - Implement `deletePlan(id: string)`: DELETE `/api/training-plans/{id}` → Observable<void>
    - _Requirements: 8.1, 8.9, 9.6, 9.7, 9.8, 9.9, 11.3, 11.6_

- [ ] 9. Implement Plans Page and routing
  - [ ] 9.1 Create `PlansPageComponent` in `src/app/features/training-plans/`
    - Standalone component with PrimeNG TabView containing two tabs: "Active Plan" and "My Plans"
    - Default to "Active Plan" tab on load
    - Provide "Create Plan" button accessible from both tabs that opens PlanCreationFormComponent dialog
    - _Requirements: 10.2, 10.3, 10.4, 10.5, 11.1_

  - [ ] 9.2 Add route configuration in `src/app/app.routes.ts`
    - Add lazy-loaded route for `/plans` pointing to PlansPageComponent
    - Apply authGuard canActivate
    - _Requirements: 10.1, 10.6, 10.7_

  - [ ] 9.3 Add "Training Plans" entry to the navigation menu
    - Add enabled, clickable menu item navigating to `/plans` route
    - _Requirements: 10.1_

- [ ] 10. Implement Active Plan View
  - [ ] 10.1 Create `ActivePlanViewComponent` in `src/app/features/training-plans/active-plan-view/`
    - Standalone component that calls TrainingPlanService.getActivePlan() on init
    - Display plan metadata header: event name, distance, duration, race date (DD/MM/YYYY), target pace (MM:SS/km via PaceFormatUtils.secondsToPace()), training days as comma-separated day names (e.g., "Mon, Wed, Fri, Sun"), long run day as day name (e.g., "Long Run: Saturday")
    - Render WeekCardComponents for each week in chronological order
    - Show "No active training plan" message with "Create Plan" button when no active plan exists (404 response)
    - Display PrimeNG ProgressSpinner while loading
    - Display PrimeNG Toast error notification on API error
    - _Requirements: 8.1, 8.2, 8.3, 8.7, 8.8, 8.9_

  - [ ] 10.2 Create `WeekCardComponent` in `src/app/features/training-plans/week-card/`
    - Standalone component displaying week summary: week number, workout count, comma-separated distinct workout types
    - Accordion-style expand/collapse on click to reveal individual workout cards
    - Each workout card shows: workout name, scheduled day name, workout structure summary
    - Emit event on expand so parent can collapse other cards (only one expanded at a time)
    - _Requirements: 8.3, 8.4, 8.5, 8.6, 8.10_

- [ ] 11. Implement Plan List View
  - [ ] 11.1 Create `PlanListViewComponent` in `src/app/features/training-plans/plan-list-view/`
    - Standalone component that calls TrainingPlanService.getPlans() and filters out ACTIVE plans
    - Display plans with: event name, distance, duration, race date, state badge (PrimeNG Tag with distinct colour per state: NEW, COMPLETED, TERMINATED)
    - Order plans by createdAt descending
    - "Activate" button: enabled for NEW/COMPLETED, disabled for TERMINATED
    - "Delete" button: enabled for NEW/COMPLETED/TERMINATED
    - Confirmation dialog (PrimeNG ConfirmDialog) before activate/delete actions with appropriate warning messages
    - On activate success: remove plan from list, show success toast, add previously active plan to list with TERMINATED state
    - On delete success: remove plan from list, show success toast
    - Show "No plans" message with create button when list is empty
    - Disable action buttons and show loading indicator during operations
    - Show error toast on API error and re-enable buttons
    - Display loading indicator while fetching plan data
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 9.9, 9.10, 9.11, 9.12, 9.13_

- [ ] 12. Implement Plan Creation Form
  - [ ] 12.1 Create `PlanCreationFormComponent` in `src/app/features/training-plans/plan-creation-form/`
    - Standalone component rendered as PrimeNG Dialog
    - Reactive form with fields: eventName (text, required, max 100), distance (dropdown: 5K/10K/Half-Marathon/Marathon), duration (dropdown: 8 Weeks/10 Weeks/12 Weeks), raceDate (date picker, future dates only), targetPace (input in MM:SS/km format, converted to seconds via PaceFormatUtils.paceToSeconds() before submission, validated 150–900 seconds), aiModel (dropdown: ChatGPT/Claude/Gemini/Kiro/Dummy), trainingDays (multi-select: Monday–Sunday, at least 1 required), longRunDay (dropdown filtered to selected trainingDays, disabled until at least one training day selected)
    - Client-side validation with inline error messages for each invalid field
    - On valid submit: call TrainingPlanService.createPlan(), show success toast, close dialog, switch to "My Plans" tab
    - On API error: show error toast with message from response
    - Disable submit button and show loading indicator during request
    - "Cancel" button closes dialog without action
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.8_

- [ ] 13. Checkpoint — Ensure frontend compiles and unit tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 14. Write frontend unit tests
  - [ ]* 14.1 Write unit tests for `TrainingPlanService` (`training-plan.service.spec.ts`)
    - Test all HTTP methods (getPlans, getPlan, getActivePlan, createPlan, activatePlan, completePlan, terminatePlan, deletePlan)
    - Test error handling and response mapping
    - _Requirements: 8.1, 8.9, 9.6, 9.7, 9.8, 9.9, 11.3, 11.6_

  - [ ]* 14.2 Write unit tests for `PaceFormatUtils` (`pace-format.utils.spec.ts`)
    - Test secondsToPace conversion (300 → "5:00", 265 → "4:25", edge cases)
    - Test paceToSeconds conversion ("5:00" → 300, "4:25" → 265, invalid input → null)
    - Test isValidPace for boundary values (150s = "2:30", 900s = "15:00", outside range)
    - _Requirements: 11.2_

  - [ ]* 14.3 Write unit tests for `PlansPageComponent` (`plans-page.component.spec.ts`)
    - Test tab switching between Active Plan and My Plans
    - Test default tab selection (Active Plan)
    - Test Create Plan button visibility and dialog opening
    - _Requirements: 10.2, 10.3, 10.4, 10.5, 11.1_

  - [ ]* 14.4 Write unit tests for `ActivePlanViewComponent` (`active-plan-view.component.spec.ts`)
    - Test metadata display (event name, distance, duration, race date, pace, training days, long run day)
    - Test week card rendering and accordion expand/collapse behaviour
    - Test loading state (spinner displayed)
    - Test error state (toast notification)
    - Test empty state ("No active training plan" message with Create Plan button)
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.6, 8.7, 8.8, 8.9, 8.10_

  - [ ]* 14.5 Write unit tests for `PlanListViewComponent` (`plan-list-view.component.spec.ts`)
    - Test plan list rendering with state badges (correct colours per state)
    - Test button enable/disable based on plan state
    - Test confirmation dialogs for activate and delete
    - Test success/error notifications
    - Test loading states during operations
    - Test empty state message
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 9.9, 9.10, 9.11, 9.12, 9.13_

  - [ ]* 14.6 Write unit tests for `PlanCreationFormComponent` (`plan-creation-form.component.spec.ts`)
    - Test form validation (required fields, eventName length, pace range, future date, trainingDays non-empty, longRunDay in trainingDays)
    - Test submission flow (loading state, success close, error display)
    - Test longRunDay dropdown filtering based on trainingDays selection
    - Test cancel button closes dialog
    - _Requirements: 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.8_

  - [ ]* 14.7 Write property test for invalid creation payload (client-side) (`plan-creation-form.component.spec.ts`)
    - **Property 2: Invalid creation payload (client-side validation)**
    - **Validates: Requirements 11.4**
    - Generate invalid form values (blank eventName, pace outside 150–900, past dates, empty trainingDays, longRunDay not in trainingDays) using fast-check, verify form is invalid and submit is blocked
    - _Uses fast-check with 100 iterations_

  - [ ]* 14.8 Write property test for plan list ordering (client-side) (`plan-list-view.component.spec.ts`)
    - **Property 5: Plan list ordering (client-side)**
    - **Validates: Requirements 9.2**
    - Generate N plan summaries with random createdAt values using fast-check, verify rendered order matches createdAt descending
    - _Uses fast-check with 100 iterations_

- [ ] 15. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Backend PBT uses jqwik (already in `pom.xml`) with `@Property(tries = 100)`
- Frontend PBT uses fast-check (already available as dev dependency) with 100 iterations
- Checkpoints ensure incremental validation at service layer, full backend, and frontend boundaries
- The Flyway migration is V4 since V1 (users), V2 (athlete profiles), and V3 (workouts) already exist
- `SecurityConfig` already requires authentication on all non-`/api/auth/**` endpoints, so `/api/training-plans/**` is automatically protected
- The `TrainingPlanService` state machine logic is a pure business logic layer — ideal target for property-based testing
- Ownership checks use `findByIdAndUserId` — returns 404 (not 403) to avoid leaking existence information
- Invalid UUID path variables are caught and mapped to 404 (same as "not found")
- The `@Transactional` annotation on `activatePlan` ensures terminate-then-activate is atomic
- PaceFormatUtils is shared between creation form (input conversion) and display components (output conversion)
- The longRunDay dropdown in the creation form dynamically filters based on trainingDays selection

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3"] },
    { "id": 2, "tasks": ["1.4", "2.1"] },
    { "id": 3, "tasks": ["2.2"] },
    { "id": 4, "tasks": ["2.3", "2.4", "2.5", "2.6", "4.1"] },
    { "id": 5, "tasks": ["4.2", "5.1"] },
    { "id": 6, "tasks": ["4.3", "4.4", "4.5", "5.2", "5.3"] },
    { "id": 7, "tasks": ["7.1"] },
    { "id": 8, "tasks": ["8.1", "8.2"] },
    { "id": 9, "tasks": ["8.3"] },
    { "id": 10, "tasks": ["9.1", "9.2", "9.3"] },
    { "id": 11, "tasks": ["10.1", "10.2", "11.1"] },
    { "id": 12, "tasks": ["12.1"] },
    { "id": 13, "tasks": ["14.1", "14.2", "14.3", "14.4", "14.5", "14.6", "14.7", "14.8"] }
  ]
}
```
