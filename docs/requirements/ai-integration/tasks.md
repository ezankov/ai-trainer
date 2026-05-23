# Implementation Plan: ai-integration

## Overview

Integrate real AI models (ChatGPT, Claude, Gemini, Kiro) into the training plan generation flow. The implementation introduces a `com.trainer.ai` package with a strategy + factory pattern (`AiPlanGenerator` interface, `AiPlanGeneratorFactory`), Spring AI `ChatClient` with `@Tool` annotation for athlete profile retrieval, structured JSON response parsing/validation/mapping, per-model configuration via `@ConfigurationProperties`, and a comprehensive exception hierarchy. The existing `DummyPlanGenerator` is refactored to implement the new interface, and `TrainingPlanService` delegates to the factory instead of directly referencing the dummy generator.

## Tasks

- [x] 1. Add Spring AI dependencies and configuration properties
  - [x] 1.1 Add Spring AI dependencies to `pom.xml`
    - Add Spring AI BOM for dependency management
    - Add `spring-ai-openai-spring-boot-starter` (for ChatGPT and Kiro)
    - Add `spring-ai-anthropic-spring-boot-starter` (for Claude)
    - Add `spring-ai-vertex-ai-gemini-spring-boot-starter` (for Gemini)
    - Add `spring-ai-test` as test dependency
    - _Requirements: 1.4, 5.6_

  - [x] 1.2 Create `AiModelProperties` configuration class in `com.trainer.ai`
    - Create `AiModelProperties` record with `@ConfigurationProperties(prefix = "trainer.ai")`
    - Define nested `ModelConfig` record with fields: `enabled` (boolean), `apiKey` (String), `model` (String)
    - Add `isAvailable()` method: returns true only if enabled AND apiKey is non-null and non-blank
    - Add properties for chatgpt, claude, gemini, kiro
    - _Requirements: 5.1, 5.2, 5.4, 5.5_

  - [x] 1.3 Add default configuration to `application.yml`
    - Add `trainer.ai.chatgpt.*`, `trainer.ai.claude.*`, `trainer.ai.gemini.*`, `trainer.ai.kiro.*` properties
    - Set all `enabled` flags to `false` by default
    - Map API keys from environment variables (`${OPENAI_API_KEY:}`, `${ANTHROPIC_API_KEY:}`, etc.)
    - Set default model identifiers (gpt-4o, claude-sonnet-4-20250514, gemini-2.0-flash, kiro-v1)
    - _Requirements: 5.1, 5.2, 5.4_

- [x] 2. Create exception hierarchy and core interface
  - [x] 2.1 Create exception classes in `com.trainer.ai`
    - Create `AiException` base class extending `RuntimeException`
    - Create `AiGenerationException` extending `AiException` (AI provider HTTP errors)
    - Create `AiGenerationTimeoutException` extending `AiException` (60s timeout)
    - Create `AiResponseValidationException` extending `AiException` (invalid response fields)
    - Create `AiResponseParseException` extending `AiException` (non-JSON or wrong schema)
    - Create `AiModelNotAvailableException` extending `AiException` (disabled or no API key)
    - Create `AiModelNotSupportedException` extending `AiException` (no implementation registered)
    - Create `AthleteProfileNotFoundException` extending `RuntimeException` (no profile for user)
    - _Requirements: 6.1, 6.2, 6.3, 6.6, 6.9, 7.5, 7.7_

  - [x] 2.2 Create `AiPlanGenerator` interface in `com.trainer.ai`
    - Define `void generate(TrainingPlan plan)` method
    - Add Javadoc specifying the contract: persists Workout and PlanWorkout entities as side-effect
    - _Requirements: 1.1, 1.7_

  - [x] 2.3 Extend `GlobalExceptionHandler` for AI exceptions
    - Map `AiModelNotAvailableException` → HTTP 400
    - Map `AiModelNotSupportedException` → HTTP 400
    - Map `AthleteProfileNotFoundException` → HTTP 400
    - Map `AiGenerationException` → HTTP 502
    - Map `AiGenerationTimeoutException` → HTTP 504
    - Map `AiResponseValidationException` → HTTP 502
    - Map `AiResponseParseException` → HTTP 502
    - Ensure error responses never expose raw AI API responses or stack traces
    - _Requirements: 6.1, 6.6, 6.9, 7.5, 7.7, 7.8_

- [x] 3. Checkpoint — Ensure project compiles with new dependencies and exception classes
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement AI response DTOs and response parsing
  - [x] 4.1 Create AI response DTO records in `com.trainer.ai`
    - Create `AiPlanResponse` record with `List<AiWorkoutResponse> workouts`
    - Create `AiWorkoutResponse` record with `name`, `sportType`, `subSport`, `steps`, `schedule`
    - Create `AiWorkoutStepResponse` record with `stepOrder`, `stepName`, `intensity`, `durationType`, `durationValue`, `targetType`, `targetValueLow`, `targetValueHigh`
    - Create `AiScheduleResponse` record with `weekNumber`, `dayOfWeek`, `orderInDay`
    - _Requirements: 3.1, 3.2, 3.3_

  - [x] 4.2 Implement response parsing logic in `SpringAiPlanGenerator` (or a dedicated parser class)
    - Parse JSON string into `AiPlanResponse` using Jackson `ObjectMapper`
    - Throw `AiResponseParseException` if JSON is malformed or does not match schema
    - Throw `AiResponseValidationException` if `workouts` array is empty or missing
    - _Requirements: 3.6, 3.11_

  - [ ]* 4.3 Write property test for AI response DTO serialization round-trip
    - **Property 3: AI response DTO serialization round-trip**
    - **Validates: Requirements 3.2, 3.3**
    - For any valid `AiPlanResponse` (valid enum strings, value ranges), serialize to JSON and deserialize back; verify all fields match
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` valid AiPlanResponse arbitraries_

  - [ ]* 4.4 Write property test for invalid JSON rejection
    - **Property 5: Invalid JSON responses are rejected with parse exception**
    - **Validates: Requirements 3.6, 3.11**
    - For any string that is not valid JSON or does not conform to AiPlanResponse schema, verify `AiResponseParseException` is thrown
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` invalid string arbitraries_

- [x] 5. Implement AI response validator
  - [x] 5.1 Create `AiResponseValidator` in `com.trainer.ai`
    - Validate `workouts` array is non-empty
    - Validate each workout `name` is non-blank and max 50 characters
    - Validate each workout has at least 1 step
    - Validate `sportType` matches `SportType` enum
    - Validate `subSport` is null or matches `SubSport` enum
    - Validate each step `intensity` matches `Intensity` enum (ACTIVE, REST, WARMUP, COOLDOWN, RECOVERY, INTERVAL)
    - Validate each step `durationType` matches `DurationType` enum (TIME, DISTANCE, REPEAT_UNTIL_STEPS_COMPLETE)
    - Validate each step `targetType` matches `TargetType` enum (SPEED, HEART_RATE, CADENCE, POWER, OPEN)
    - Validate `weekNumber` is between 1 and `plan.getDuration().getWeeks()`
    - Validate `dayOfWeek` is between 1 and 7
    - Validate `orderInDay` is between 1 and 10
    - Validate `stepOrder` values are sequential starting at 1
    - Validate `durationValue` is within valid ranges per `durationType`
    - Throw `AiResponseValidationException` with details about which fields failed
    - _Requirements: 3.7, 3.8, 3.9, 6.3_

  - [ ]* 5.2 Write property test for validator rejects invalid values
    - **Property 6: Validator rejects out-of-range and invalid enum values**
    - **Validates: Requirements 3.7, 3.8, 3.9, 6.3**
    - For any `AiPlanResponse` with one invalid field injected (bad weekNumber, dayOfWeek, orderInDay, intensity, durationType, or targetType), verify `AiResponseValidationException` is thrown identifying the invalid field
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` single-field-invalid arbitraries_

- [x] 6. Implement AI response mapper
  - [x] 6.1 Create `AiResponseMapper` in `com.trainer.ai`
    - For each workout in the validated response: create `Workout` entity with `userId = plan.getUserId()`, `sportType`, `subSport`, `name`, `numValidSteps = steps.size()`
    - For each step: create `WorkoutStep` entity with `stepOrder`, `stepName`, `intensity`, `durationType`, `durationValue`, `targetType`, `targetValueLow`, `targetValueHigh`
    - Save each workout via `WorkoutRepository` (cascades to steps)
    - Create `PlanWorkout` entity linking workout to plan with `weekNumber`, `dayOfWeek`, `orderInDay`
    - Save all `PlanWorkout` entities via `PlanWorkoutRepository`
    - _Requirements: 3.4, 3.5, 3.10, 7.6_

  - [ ]* 6.2 Write property test for response-to-entity mapping
    - **Property 4: Response-to-entity mapping preserves all fields**
    - **Validates: Requirements 3.4, 3.5, 3.10, 7.6**
    - For any valid `AiPlanResponse` and any `TrainingPlan`, verify created Workout entities have matching name, sportType, numValidSteps, userId; WorkoutSteps match all fields; PlanWorkouts match schedule fields
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` valid response + plan arbitraries, mocked repositories_

- [x] 7. Checkpoint — Ensure response parsing, validation, and mapping compile and tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement athlete profile tool and prompt builder
  - [x] 8.1 Create `AthleteProfileToolResponse` record in `com.trainer.ai`
    - Fields: `dateOfBirth`, `weightKg`, `restingHR`, `maxHR`, `lthr`, `thresholdPaceSecondsPerKm`, `vo2Max`, `fiveKSeconds`, `tenKSeconds`, `halfMarathonSeconds`, `marathonSeconds`
    - Null fields included in JSON serialization
    - _Requirements: 2.2_

  - [x] 8.2 Create `AthleteProfileTool` in `com.trainer.ai`
    - Annotate method with `@Tool` with description instructing the model to call it before generating
    - Accept `ToolContext` parameter to extract `userId`
    - Retrieve `AthleteProfile` from `AthleteProfileRepository` by userId
    - Throw `AthleteProfileNotFoundException` if no profile exists
    - Map entity to `AthleteProfileToolResponse`
    - _Requirements: 2.1, 2.3, 2.4, 2.5, 2.7, 2.8_

  - [ ]* 8.3 Write property test for athlete profile tool completeness
    - **Property 1: Athlete profile tool returns complete data**
    - **Validates: Requirements 2.1, 2.2**
    - For any `AthleteProfile` with arbitrary null/non-null optional fields, verify returned `AthleteProfileToolResponse` contains all fields matching entity values exactly
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` random AthleteProfile arbitraries_

  - [ ]* 8.4 Write property test for athlete profile user isolation
    - **Property 2: Athlete profile tool enforces user isolation**
    - **Validates: Requirements 2.8**
    - For any two distinct users with profiles, verify tool invoked with userId=A returns only A's data
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` pairs of random profiles_

  - [x] 8.5 Create training principles resource file
    - Copy `docs/requirements/ai-integration/training-principles.md` to `src/main/resources/prompts/training-principles.md`
    - _Requirements: 4.9, 4.10_

  - [x] 8.6 Create `AiPromptBuilder` in `com.trainer.ai`
    - Load training principles from `classpath:prompts/training-principles.md` at startup
    - `buildSystemPrompt()`: compose role definition + full training principles + instruction to call `getAthleteProfile` tool + JSON response schema specification
    - `buildUserPrompt(TrainingPlan plan)`: include event name, distance (human-readable), duration (weeks), race date, target pace (MM:SS/km), training days (day names), long run day (day name), instruction to generate one workout per training day per week
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10_

  - [ ]* 8.7 Write property test for user prompt contains all plan parameters
    - **Property 7: User prompt contains all plan parameters**
    - **Validates: Requirements 4.1**
    - For any valid `TrainingPlan`, verify `buildUserPrompt(plan)` contains event name, distance text, duration weeks, race date, target pace in MM:SS/km, all training day names, long run day name
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` random TrainingPlan arbitraries_

  - [ ]* 8.8 Write property test for system prompt embeds training principles
    - **Property 8: System prompt embeds training principles**
    - **Validates: Requirements 4.9**
    - Verify `buildSystemPrompt()` contains the full content of the training principles document
    - _Uses jqwik single verification (static content)_

- [x] 9. Implement SpringAiPlanGenerator and factory
  - [x] 9.1 Create `SpringAiPlanGenerator` in `com.trainer.ai`
    - Accept `ChatModel`, `AiPromptBuilder`, `AthleteProfileTool`, `AiResponseValidator`, `AiResponseMapper` as constructor dependencies
    - Implement `generate(TrainingPlan plan)`:
      1. Build `ChatClient` from `ChatModel`
      2. Call `client.prompt()` with system prompt, user prompt, tool registration (`athleteProfileTool`), and `toolContext` containing `userId`
      3. Parse response content into `AiPlanResponse`
      4. Validate via `AiResponseValidator`
      5. Map and persist via `AiResponseMapper`
    - Catch AI provider HTTP errors → wrap in `AiGenerationException`
    - Catch timeout → wrap in `AiGenerationTimeoutException`
    - Log full AI response at DEBUG level, errors at ERROR level
    - Configure 60-second timeout on the ChatClient call
    - Do NOT retry on failure
    - _Requirements: 1.1, 1.4, 1.7, 2.5, 2.6, 2.7, 6.1, 6.2, 6.7, 6.8_

  - [x] 9.2 Create `AiConfiguration` in `com.trainer.ai`
    - Create conditional beans for each AI model using `@ConditionalOnProperty`
    - `chatgptPlanGenerator`: enabled when `trainer.ai.chatgpt.enabled=true`, uses OpenAI `ChatModel`
    - `claudePlanGenerator`: enabled when `trainer.ai.claude.enabled=true`, uses Anthropic `ChatModel`
    - `geminiPlanGenerator`: enabled when `trainer.ai.gemini.enabled=true`, uses Vertex AI `ChatModel`
    - `kiroPlanGenerator`: enabled when `trainer.ai.kiro.enabled=true`, uses OpenAI `ChatModel` (with Kiro endpoint)
    - Enable `@ConfigurationProperties` for `AiModelProperties`
    - _Requirements: 5.6, 5.7_

  - [x] 9.3 Create `AiPlanGeneratorFactory` in `com.trainer.ai`
    - Inject all available `SpringAiPlanGenerator` beans (mapped by `AiModel`)
    - Inject `DummyPlanGenerator` and `AiModelProperties`
    - `getGenerator(AiModel)`:
      - `DUMMY` → return `DummyPlanGenerator` (no config check)
      - Other models → check `isAvailable()` on corresponding `ModelConfig`; throw `AiModelNotAvailableException` if disabled/unconfigured; throw `AiModelNotSupportedException` if no bean registered
    - _Requirements: 1.2, 1.3, 1.5, 5.3, 5.5, 5.7_

  - [ ]* 9.4 Write property test for factory rejects disabled models
    - **Property 9: Factory rejects disabled or unconfigured models**
    - **Validates: Requirements 5.3, 5.5**
    - For any non-DUMMY `AiModel` with enabled=false or API key null/empty/whitespace, verify `AiModelNotAvailableException` is thrown
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` random AiModel + config combinations_

- [x] 10. Refactor DummyPlanGenerator and TrainingPlanService
  - [x] 10.1 Modify `DummyPlanGenerator` to implement `AiPlanGenerator`
    - Add `implements AiPlanGenerator` to class declaration
    - Add `@Override` annotation to existing `generate(TrainingPlan plan)` method
    - No changes to internal logic
    - _Requirements: 1.3_

  - [x] 10.2 Modify `TrainingPlanService` to use `AiPlanGeneratorFactory`
    - Replace `DummyPlanGenerator` dependency with `AiPlanGeneratorFactory`
    - In `createPlan()`: add pre-condition check — if aiModel is not DUMMY, verify athlete profile exists (throw `AthleteProfileNotFoundException` if missing)
    - Replace `if (aiModel == AiModel.DUMMY) { dummyPlanGenerator.generate(saved); }` with `AiPlanGenerator generator = aiPlanGeneratorFactory.getGenerator(aiModel); generator.generate(saved);`
    - Ensure `@Transactional` boundary covers the entire flow (already present)
    - _Requirements: 1.6, 6.4, 6.5, 7.1, 7.2, 7.3, 7.5, 7.8_

  - [ ]* 10.3 Write property test for transaction rollback on failure
    - **Property 10: Transaction rollback on generation failure**
    - **Validates: Requirements 6.4, 7.8**
    - For any exception thrown by `AiPlanGenerator.generate()`, verify no Workout or PlanWorkout entities are persisted for the plan, and the TrainingPlan itself is not persisted
    - _Uses jqwik `@Property(tries = 100)` with mocked generator throwing random AI exceptions_

- [x] 11. Checkpoint — Ensure full integration compiles and existing tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 12. Write unit tests for AI components
  - [ ]* 12.1 Write unit tests for `AiPlanGeneratorFactory`
    - Test DUMMY resolution returns DummyPlanGenerator without config check
    - Test each AI model resolution when enabled and API key present
    - Test `AiModelNotAvailableException` when model disabled
    - Test `AiModelNotAvailableException` when API key is null/empty/whitespace
    - Test `AiModelNotSupportedException` when no bean registered
    - _Requirements: 1.2, 1.3, 1.5, 5.3, 5.5_

  - [ ]* 12.2 Write unit tests for `SpringAiPlanGenerator`
    - Test successful generation flow with mocked ChatClient
    - Test AI provider HTTP error → `AiGenerationException`
    - Test timeout → `AiGenerationTimeoutException`
    - Test parse failure → `AiResponseParseException`
    - Test validation failure → `AiResponseValidationException`
    - Verify no retry on failure
    - Verify logging at DEBUG (response) and ERROR (failure) levels
    - _Requirements: 6.1, 6.2, 6.7, 6.8_

  - [ ]* 12.3 Write unit tests for `AiPromptBuilder`
    - Test system prompt contains training principles content
    - Test system prompt contains JSON schema specification
    - Test system prompt contains instruction to call getAthleteProfile
    - Test user prompt contains all plan parameters with correct formatting
    - Test pace formatting (seconds → MM:SS/km)
    - Test day number → day name conversion
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.8, 4.9_

  - [ ]* 12.4 Write unit tests for `AthleteProfileTool`
    - Test successful retrieval returns correct data
    - Test user not found throws `AthleteProfileNotFoundException`
    - Test null optional fields are preserved in response
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [ ]* 12.5 Write unit tests for `AiResponseValidator`
    - Test valid response passes validation
    - Test empty workouts array → exception
    - Test invalid enum values → exception with field details
    - Test weekNumber out of range → exception
    - Test dayOfWeek out of range → exception
    - Test orderInDay out of range → exception
    - Test missing required fields → exception
    - _Requirements: 3.7, 3.8, 3.9, 3.11, 6.3_

  - [ ]* 12.6 Write unit tests for `AiResponseMapper`
    - Test correct Workout entity creation (name, sportType, subSport, numValidSteps, userId)
    - Test correct WorkoutStep creation (all fields mapped)
    - Test correct PlanWorkout creation (weekNumber, dayOfWeek, orderInDay)
    - Test userId propagation from plan to workouts
    - _Requirements: 3.4, 3.5, 3.10_

  - [ ]* 12.7 Write unit tests for `TrainingPlanService` AI integration
    - Test delegation to factory for non-DUMMY models
    - Test pre-condition check: no athlete profile → `AthleteProfileNotFoundException`
    - Test disabled model → `AiModelNotAvailableException` → HTTP 400
    - Test generation failure → transaction rollback (no entities persisted)
    - Test DUMMY model still works without athlete profile
    - _Requirements: 7.1, 7.2, 7.3, 7.5, 7.7, 7.8_

- [ ] 13. Write integration tests
  - [ ]* 13.1 Write Spring Boot integration test for AI plan generation flow
    - Test successful plan generation end-to-end with mocked AI provider (MockWebServer or WireMock)
    - Test AI provider error → HTTP 502 response
    - Test AI provider timeout → HTTP 504 response
    - Test no athlete profile for non-DUMMY model → HTTP 400
    - Test disabled model → HTTP 400
    - Test DUMMY model continues to work unchanged
    - Verify transaction rollback: no Workout/PlanWorkout entities on failure
    - Verify response structure is identical regardless of AI model used
    - _Requirements: 6.1, 6.2, 6.4, 6.5, 6.6, 6.9, 7.1, 7.2, 7.3, 7.4, 7.5, 7.7, 7.8_

- [x] 14. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The project uses jqwik 1.9.3 (already in `pom.xml`) for property-based testing with `@Property(tries = 100)`
- Spring AI test utilities (`spring-ai-test`) are needed for mocking `ChatModel` and `ChatClient`
- The `DummyPlanGenerator` refactoring is backward-compatible — it implements the new interface without changing behaviour
- The `@Transactional` boundary on `TrainingPlanService.createPlan()` already exists; the AI generation runs within it
- All AI models share the same `SpringAiPlanGenerator` class — only the injected `ChatModel` differs
- The training principles file is copied from `docs/requirements/ai-integration/` to `src/main/resources/prompts/` as a classpath resource
- Kiro uses the OpenAI-compatible API (same starter as ChatGPT, different endpoint/key)
- No Flyway migration needed — this feature uses existing tables (workouts, workout_steps, plan_workouts, training_plans)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "2.2"] },
    { "id": 2, "tasks": ["2.3"] },
    { "id": 3, "tasks": ["4.1", "4.2"] },
    { "id": 4, "tasks": ["4.3", "4.4", "5.1"] },
    { "id": 5, "tasks": ["5.2", "6.1"] },
    { "id": 6, "tasks": ["6.2", "8.1", "8.5"] },
    { "id": 7, "tasks": ["8.2", "8.6"] },
    { "id": 8, "tasks": ["8.3", "8.4", "8.7", "8.8"] },
    { "id": 9, "tasks": ["9.1"] },
    { "id": 10, "tasks": ["9.2", "9.3"] },
    { "id": 11, "tasks": ["9.4", "10.1"] },
    { "id": 12, "tasks": ["10.2"] },
    { "id": 13, "tasks": ["10.3"] },
    { "id": 14, "tasks": ["12.1", "12.2", "12.3", "12.4", "12.5", "12.6", "12.7"] },
    { "id": 15, "tasks": ["13.1"] }
  ]
}
```
