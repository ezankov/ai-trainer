# Requirements Document

## Introduction

This feature integrates real AI models (ChatGPT, Claude, Gemini, Kiro) into the training plan generation flow. Currently, the application uses a `DummyPlanGenerator` that creates placeholder workouts when `aiModel = DUMMY`. The new feature introduces a generic AI plan generation interface that delegates to model-specific implementations based on the `CreateTrainingPlanRequest.aiModel` field. Athlete profile data is provided to the AI model via MCP (Model Context Protocol) tool calls rather than being embedded directly in the prompt. The AI model responds in a structured format that maps directly to the existing Workout and PlanWorkout entity structure.

## Glossary

- **AI_Plan_Generator**: A generic interface (Java interface) defining the contract for generating training plan workouts from a TrainingPlan entity. Each AI model has its own implementation of this interface.
- **AI_Plan_Generator_Factory**: A Spring component responsible for resolving the correct AI_Plan_Generator implementation based on the AiModel enum value from the CreateTrainingPlanRequest.
- **MCP_Server**: A Spring AI MCP server endpoint that exposes athlete profile data as a tool callable by the AI model during plan generation. Uses Spring AI's native MCP integration (spring-ai-mcp).
- **MCP_Tool**: A function exposed via the MCP_Server that the AI model can invoke to retrieve contextual data (e.g., athlete profile) during the generation conversation.
- **Structured_Response**: A JSON response format that the AI model produces, containing a list of workout definitions with their steps and scheduling assignments (week number, day of week, order in day) that map directly to the Workout and PlanWorkout entities.
- **AI_Prompt**: The system and user prompt sent to the AI model containing the training plan parameters (event name, distance, duration, race date, target pace, training days, long run day) and instructions for structured output. The prompt is identical across all AI model implementations.
- **Spring_AI**: The Spring AI framework used to interact with AI model APIs (OpenAI, Anthropic, Google Vertex AI) in a unified manner.
- **Training_Plan_Service**: The existing backend service that orchestrates plan creation. Extended to delegate to AI_Plan_Generator implementations for non-DUMMY models.
- **Athlete_Profile**: The existing entity containing the athlete's biometric data and race times, retrieved by the MCP_Tool when the AI model requests it.
- **Workout**: The existing entity representing a structured training session with ordered WorkoutSteps.
- **Workout_Step**: The existing entity representing a single step within a Workout, with intensity, duration, and target parameters.
- **Plan_Workout**: The existing join entity linking a Training_Plan to a Workout with scheduling metadata (weekNumber, dayOfWeek, orderInDay).

---

## Requirements

### Requirement 1: Generic AI Plan Generator Interface

**User Story:** As a developer, I want a generic interface for AI plan generation, so that different AI model implementations can be swapped in without changing the core generation logic.

#### Acceptance Criteria

1. THE AI_Plan_Generator interface SHALL define a single method that accepts a TrainingPlan entity and persists the generated Workout and PlanWorkout entities for that plan as a side-effect (void return type).
2. THE AI_Plan_Generator_Factory SHALL resolve the correct AI_Plan_Generator implementation based on the AiModel enum value (CHATGPT, CLAUDE, GEMINI, KIRO, DUMMY).
3. WHEN the AiModel value is DUMMY, THE AI_Plan_Generator_Factory SHALL return the existing DummyPlanGenerator implementation (which implements the AI_Plan_Generator interface) without modification to its behaviour.
4. WHEN the AiModel value is CHATGPT, CLAUDE, GEMINI, or KIRO, THE AI_Plan_Generator_Factory SHALL return the corresponding AI-backed implementation that uses Spring_AI to communicate with the model's API.
5. IF the AI_Plan_Generator_Factory cannot resolve an implementation for the requested AiModel value (e.g., no registered implementation exists), THEN THE AI_Plan_Generator_Factory SHALL throw an exception indicating the requested AI model is not supported.
6. THE Training_Plan_Service SHALL delegate plan generation to the AI_Plan_Generator resolved by the AI_Plan_Generator_Factory, replacing the current direct reference to DummyPlanGenerator and the conditional logic.
7. THE AI_Prompt content and structure SHALL be identical across all non-DUMMY AI_Plan_Generator implementations; only the underlying model client configuration SHALL differ between implementations.

---

### Requirement 2: MCP Context Passing for Athlete Profile

**User Story:** As a developer, I want the athlete profile data to be passed to the AI model via MCP tool calls, so that the model can request only the context it needs rather than receiving all data upfront in the prompt.

#### Acceptance Criteria

1. THE MCP_Server SHALL expose an MCP_Tool named `getAthleteProfile` that accepts no parameters and returns the Athlete_Profile data for the authenticated user of the current plan generation session.
2. THE MCP_Tool SHALL return the athlete profile as a structured JSON object containing: dateOfBirth, weightKg, restingHR, maxHR, lthr, thresholdPaceSecondsPerKm, vo2Max, fiveKSeconds, tenKSeconds, halfMarathonSeconds, and marathonSeconds. Fields that have no value stored in the database SHALL be included in the JSON response with a null value.
3. WHEN the AI model invokes the `getAthleteProfile` MCP_Tool during plan generation, THE MCP_Server SHALL retrieve the Athlete_Profile from the database for the authenticated user of the current session and return it to the model within 5 seconds.
4. IF the `getAthleteProfile` MCP_Tool is invoked for a user who does not have an Athlete_Profile, THEN THE MCP_Server SHALL return an error message indicating no athlete profile exists for the user.
5. THE AI_Prompt SHALL NOT include the athlete profile data directly; instead, the prompt SHALL instruct the AI model to call the `getAthleteProfile` tool to retrieve the athlete's fitness data.
6. THE MCP_Server SHALL use Spring AI's native MCP integration (spring-ai-mcp) for tool registration and invocation handling.
7. THE MCP_Server SHALL only expose the `getAthleteProfile` tool within the scope of the current plan generation session, ensuring the tool is registered at the start of the AI generation call and unregistered or inaccessible after the call completes.
8. THE MCP_Server SHALL enforce that the `getAthleteProfile` tool always returns data for the authenticated user who initiated the plan generation request, preventing retrieval of any other user's Athlete_Profile regardless of tool invocation parameters.

---

### Requirement 3: Structured AI Response Format

**User Story:** As a developer, I want the AI model to respond in a structured format, so that the application can reliably parse the response and create Workout and PlanWorkout entities.

#### Acceptance Criteria

1. THE AI_Prompt SHALL instruct the AI model to respond with a JSON object containing a `workouts` array, where each entry defines a workout with its steps and scheduling assignments.
2. EACH workout entry in the Structured_Response SHALL contain: `name` (string, max 50 characters), `sportType` (string, value "RUNNING"), `subSport` (string matching a valid SubSport enum value, or null), `steps` (array of at least 1 step object), and `schedule` (object with `weekNumber`, `dayOfWeek`, `orderInDay` where `orderInDay` is an integer between 1 and 10 inclusive).
3. EACH step object in the Structured_Response SHALL contain: `stepOrder` (integer, 1-based), `stepName` (string, max 50 characters), `intensity` (one of ACTIVE, REST, WARMUP, COOLDOWN, RECOVERY, INTERVAL), `durationType` (one of TIME, DISTANCE, REPEAT_UNTIL_STEPS_COMPLETE), `durationValue` (integer, 1 to 86400 for TIME in seconds, 1 to 100000 for DISTANCE in metres, or step count for REPEAT_UNTIL_STEPS_COMPLETE), `targetType` (one of SPEED, HEART_RATE, CADENCE, POWER, OPEN), `targetValueLow` (integer or null), `targetValueHigh` (integer or null).
4. THE AI_Plan_Generator SHALL parse the Structured_Response and create Workout entities with their WorkoutSteps, setting `numValidSteps` to the count of steps in each workout.
5. THE AI_Plan_Generator SHALL create PlanWorkout entities linking each generated Workout to the TrainingPlan using the `weekNumber`, `dayOfWeek`, and `orderInDay` from the schedule field in the Structured_Response.
6. IF the AI model returns a response that does not conform to the expected Structured_Response format, THEN THE AI_Plan_Generator SHALL throw a descriptive exception indicating the response could not be parsed.
7. THE AI_Plan_Generator SHALL validate that all `weekNumber` values in the response are between 1 and the plan's duration in weeks (inclusive), where duration in weeks is derived from the TrainingPlan's PlanDuration enum value.
8. THE AI_Plan_Generator SHALL validate that all `dayOfWeek` values in the response are between 1 and 7 (inclusive).
9. THE AI_Plan_Generator SHALL validate that all `intensity`, `durationType`, and `targetType` values in the response match the defined enum values in the application.
10. THE AI_Plan_Generator SHALL set the `userId` on each generated Workout to the owner of the TrainingPlan.
11. IF the Structured_Response contains an empty `workouts` array or the `workouts` field is missing, THEN THE AI_Plan_Generator SHALL throw a validation exception indicating that at least one workout is required.

---

### Requirement 4: AI Prompt Construction

**User Story:** As a developer, I want a well-defined prompt template for AI plan generation, so that all AI models receive consistent instructions and produce comparable training plans.

#### Acceptance Criteria

1. THE AI_Prompt SHALL include the following training plan parameters: event name, distance (as human-readable text e.g. "Half Marathon"), duration (as number of weeks e.g. "12 weeks"), race date, target pace (in MM:SS/km format), training days (as day names e.g. "Monday, Wednesday, Friday, Sunday"), and long run day (as day name e.g. "Saturday").
2. THE AI_Prompt SHALL instruct the AI model to call the `getAthleteProfile` MCP_Tool to retrieve the athlete's fitness data before generating the plan.
3. THE AI_Prompt SHALL specify the exact JSON response schema that the AI model must follow, matching the Structured_Response format defined in Requirement 3.
4. THE AI_Prompt SHALL instruct the AI model to generate one workout per training day per week, covering all weeks in the plan duration.
5. THE AI_Prompt SHALL instruct the AI model to assign the long run workout to the specified long run day each week.
6. THE AI_Prompt SHALL instruct the AI model to use workout step structures that include at minimum one step with intensity WARMUP, one or more steps with intensity ACTIVE or INTERVAL for the main effort, and one step with intensity COOLDOWN.
7. THE AI_Prompt SHALL instruct the AI model to set target values (pace or heart rate) based on the athlete's profile data retrieved via the MCP_Tool.
8. THE AI_Prompt SHALL be defined as a reusable template in a dedicated class or resource file, not hardcoded within individual AI_Plan_Generator implementations.
9. THE AI_Prompt system message SHALL embed the full training principles document (`training-principles.md`) as coaching context, providing the AI model with evidence-based guidelines on intensity distribution (80/20 rule), periodization (base/build/taper phases), workout types, heart rate zones, progressive overload, and distance-specific training strategies.
10. THE training principles document SHALL be stored as a classpath resource file (e.g., `src/main/resources/prompts/training-principles.md`) and loaded at application startup, so that updates to coaching guidelines do not require code changes to the prompt template class.

---

### Requirement 5: Model-Specific Configuration

**User Story:** As a developer, I want each AI model to have its own configuration (API keys, endpoints, model identifiers), so that the application can connect to multiple AI providers.

#### Acceptance Criteria

1. THE application SHALL support configuration properties for each AI model under a dedicated namespace (e.g., `trainer.ai.chatgpt.*`, `trainer.ai.claude.*`, `trainer.ai.gemini.*`, `trainer.ai.kiro.*`).
2. EACH model configuration SHALL include at minimum: API key, model identifier (e.g., "gpt-4o", "claude-sonnet-4-20250514", "gemini-2.0-flash"), and an enabled flag that defaults to false when not explicitly configured.
3. IF the AI_Plan_Generator_Factory is asked to resolve an AI_Plan_Generator for a model whose `enabled` flag is false, THEN THE AI_Plan_Generator_Factory SHALL throw an exception indicating the requested AI model is not currently available.
4. THE application SHALL load API keys from environment variables or application properties, following Spring Boot's externalized configuration conventions.
5. IF an AI model's API key is not configured, is null, is an empty string, or contains only whitespace, THEN THE AI_Plan_Generator_Factory SHALL treat that model as disabled regardless of the enabled flag value.
6. THE application SHALL use Spring AI's auto-configuration for model clients (OpenAI for ChatGPT, Anthropic for Claude, Vertex AI for Gemini) where available.
7. THE AI_Plan_Generator_Factory SHALL resolve the DUMMY model without requiring any configuration properties (no API key, no model identifier, no enabled flag).

---

### Requirement 6: Error Handling and Resilience

**User Story:** As a developer, I want robust error handling for AI plan generation, so that failures are communicated clearly and do not leave the system in an inconsistent state.

#### Acceptance Criteria

1. IF the AI model API returns an HTTP error (4xx or 5xx), THEN THE AI_Plan_Generator SHALL throw a descriptive exception containing the error status and message from the AI provider.
2. IF the AI model API does not respond within 60 seconds, THEN THE AI_Plan_Generator SHALL abort the request and throw a timeout exception.
3. IF the AI model returns a response that fails validation (invalid enum values, week numbers out of range, missing required fields), THEN THE AI_Plan_Generator SHALL throw a validation exception with details about which fields failed.
4. WHEN an AI_Plan_Generator throws any exception during plan generation, THE Training_Plan_Service SHALL leave the TrainingPlan in state NEW with no PlanWorkout or Workout entities created for that plan.
5. THE Training_Plan_Service SHALL wrap the entire AI generation flow (API call, response parsing, entity creation) in a single database transaction so that partial failures result in a complete rollback.
6. WHEN plan generation fails due to an AI provider error (4xx or 5xx), THE Training_Plan_API SHALL return HTTP 502 with an error response body that identifies the failure category (provider error) without exposing internal stack traces or raw AI model API responses.
7. WHEN generation fails, THE AI_Plan_Generator SHALL log the full AI model response at DEBUG level and the error details at ERROR level.
8. THE AI_Plan_Generator SHALL NOT retry failed AI model API calls; each plan generation request SHALL result in at most one call to the AI model API, and failures SHALL be reported immediately to the caller.
9. IF the AI model API does not respond within 60 seconds and a timeout exception is thrown, THEN THE Training_Plan_API SHALL return HTTP 504 with an error response body indicating the AI model timed out.

---

### Requirement 7: Integration with Existing Plan Creation Flow

**User Story:** As a logged-in athlete, I want my training plan to be generated by the selected AI model when I create a plan, so that I receive a personalised training programme based on my profile and goals.

#### Acceptance Criteria

1. WHEN a POST request is sent to `/api/training-plans` with an `aiModel` value of CHATGPT, CLAUDE, GEMINI, or KIRO, THE Training_Plan_Service SHALL invoke the corresponding AI_Plan_Generator to generate workouts and schedule them into the plan.
2. WHEN AI plan generation completes successfully, THE Training_Plan_Service SHALL persist all generated Workout and PlanWorkout entities within the same database transaction as the plan creation and return the created plan with state NEW and HTTP 201.
3. WHEN a POST request is sent to `/api/training-plans` with `aiModel` value of DUMMY, THE Training_Plan_Service SHALL continue to use the existing DummyPlanGenerator without any change in behaviour.
4. THE Training_Plan_API response for a successfully created plan SHALL be identical in structure regardless of which AI model was used for generation (same response DTO with id, eventName, distance, duration, raceDate, targetPaceSecondsPerKm, aiModel, trainingDays, longRunDay, state, createdAt, updatedAt).
5. IF the authenticated user does not have an Athlete_Profile and the selected aiModel is not DUMMY, THEN THE Training_Plan_Service SHALL return HTTP 400 with an error message indicating an athlete profile is required for AI-generated plans.
6. THE AI plan generation SHALL produce workouts that conform to the same entity constraints as the DummyPlanGenerator: valid Intensity, DurationType, TargetType enum values; stepOrder starting at 1; numValidSteps matching the actual step count.
7. IF the selected aiModel is not DUMMY and the corresponding AI model is disabled (enabled flag is false or API key is not configured), THEN THE Training_Plan_Service SHALL return HTTP 400 with an error message indicating the requested AI model is not currently available.
8. IF the AI_Plan_Generator throws any exception during plan generation, THEN THE Training_Plan_Service SHALL roll back the entire transaction so that no TrainingPlan, Workout, or PlanWorkout entities are persisted, and THE Training_Plan_API SHALL return HTTP 502 with an error message indicating the AI model failed to generate the plan.
