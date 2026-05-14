# Implementation Plan: athlete-profile

## Overview

Implement the Athlete Profile feature end-to-end across the Spring Boot backend and Angular frontend. The backend provides CRUD endpoints under `/api/athlete-profile` with zone calculation logic (HR and Pace profiles). The frontend provides a `ProfilePageComponent` for creating/editing the profile and displaying calculated zones. A Flyway migration (V2) creates the required database tables.

## Tasks

- [ ] 1. Create Flyway migration and JPA entities
  - [ ] 1.1 Create Flyway migration `V2__athlete_profile_schema.sql`
    - Create tables: `athlete_profiles`, `hr_profiles`, `hr_zones`, `pace_profiles`, `pace_zones` in the `trainer` schema
    - Include all columns, constraints, foreign keys, and indexes as specified in the design
    - _Requirements: 1.1, 2.1, 4.3, 5.4_

  - [ ] 1.2 Create JPA entities in `com.trainer.profile`
    - Create `AthleteProfile` entity mapped to `trainer.athlete_profiles` with all fields
    - Create `HrProfile` entity mapped to `trainer.hr_profiles` with `@OneToMany` to `HrZone`
    - Create `HrZone` entity mapped to `trainer.hr_zones`
    - Create `PaceProfile` entity mapped to `trainer.pace_profiles` with `@OneToMany` to `PaceZone`
    - Create `PaceZone` entity mapped to `trainer.pace_zones`
    - _Requirements: 1.1, 4.3, 5.4_

  - [ ] 1.3 Create repositories in `com.trainer.profile`
    - Create `AthleteProfileRepository` with `findByUserId`, `existsByUserId`
    - Create `HrProfileRepository` with `findByAthleteProfileId`, `deleteByAthleteProfileId`
    - Create `PaceProfileRepository` with `findByAthleteProfileId`, `deleteByAthleteProfileId`
    - _Requirements: 1.1, 2.1, 3.1_

- [ ] 2. Implement zone calculators
  - [ ] 2.1 Implement `HrZoneCalculator` in `com.trainer.profile`
    - Pure function: `List<HrZone> calculate(int restingHR, int lthr, int maxHR)`
    - Implement 6-zone calculation using LTHR percentage formula from design
    - Ensure adjacent zones share boundary values
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [ ]* 2.2 Write property tests for `HrZoneCalculator` (`HrZoneCalculatorPropertyTest`)
    - **Property 3: HR zone calculation correctness** — for any valid (restingHR, lthr, maxHR) triple where restingHR < lthr ≤ maxHR, verify all 6 zones match the LTHR percentage formula
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.6**
    - **Property 4: HR zone adjacency invariant** — for any valid triple, verify upper bound of zone N equals lower bound of zone N+1
    - **Validates: Requirements 4.5**
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` valid (restingHR, lthr, maxHR) arbitraries_

  - [ ] 2.3 Implement `PaceZoneCalculator` in `com.trainer.profile`
    - Pure function: `List<PaceZone> calculate(int thresholdPaceSecondsPerKm)`
    - Convert TP to speed, apply intensity percentages, convert back to pace
    - Cap Zone 1 upper bound at 900, Zone 6 lower bound at 150
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ]* 2.4 Write property tests for `PaceZoneCalculator` (`PaceZoneCalculatorPropertyTest`)
    - **Property 5: Pace zone calculation correctness** — for any valid TP in [150, 900], verify 6 zones match the speed-percentage formula with correct caps
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4**
    - **Property 6: Pace zone ordering invariant** — for any valid TP, verify every zone has lowerBound < upperBound (faster pace < slower pace)
    - **Validates: Requirements 5.5**
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` TP arbitraries in [150, 900]_

- [ ] 3. Checkpoint — Ensure zone calculator tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. Implement DTOs and validation
  - [ ] 4.1 Create request/response DTOs in `com.trainer.profile`
    - Create `CreateProfileRequest` record with Bean Validation annotations
    - Create `UpdateProfileRequest` record (same structure as Create)
    - Create `ProfileResponse` record including nested `HrProfileResponse` and `PaceProfileResponse`
    - _Requirements: 1.1, 1.9, 1.10, 1.11, 1.12, 1.14, 2.1, 2.3, 2.4_

  - [ ] 4.2 Implement cross-field validation in `com.trainer.profile`
    - Validate `maxHR > restingHR`
    - Validate `lthr > restingHR && lthr <= maxHR` when lthr is provided
    - Validate `dateOfBirth` results in age ≥ 13 and is not in the future
    - Validate race time ordering: 5K < 10K < Half < Marathon (only between non-null adjacent pairs)
    - Collect all validation errors and return them in a single 400 response
    - _Requirements: 1.3, 1.7, 1.13, 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 5. Implement service layer
  - [ ] 5.1 Implement `AthleteProfileService` in `com.trainer.profile`
    - `createProfile(Long userId, CreateProfileRequest)`: validate, persist, trigger zone calculation if thresholds present, return 409 if profile exists
    - `getProfile(Long userId)`: fetch profile with zones, return 404 if not found
    - `updateProfile(Long userId, UpdateProfileRequest)`: full replacement, recalculate/delete zones as needed
    - _Requirements: 1.1, 1.2, 2.1, 2.2, 2.3, 2.4, 2.5, 2.7, 3.1, 3.2, 3.3, 3.5, 3.6, 3.7, 3.8, 4.1, 4.7, 4.8, 5.1, 5.6, 5.7, 5.8_

  - [ ]* 5.2 Write property tests for `AthleteProfileService` (`AthleteProfileServicePropertyTest`)
    - **Property 1: Profile data round-trip** — for any valid profile payload, creating via service and retrieving returns all submitted field values unchanged
    - **Validates: Requirements 1.1, 2.1**
    - **Property 7: Zone recalculation on update** — for any existing profile, updating with a different valid threshold produces freshly calculated zone values
    - **Validates: Requirements 3.5, 3.6, 4.7, 5.6**
    - **Property 9: Update full replacement semantics** — for any profile with all optional fields populated, updating with only required fields results in all optional fields being null
    - **Validates: Requirements 3.3**
    - _Uses jqwik `@Property(tries = 100)` with `@Provide` valid profile arbitraries; uses Mockito for repositories_

- [ ] 6. Implement controller and exception handling
  - [ ] 6.1 Create custom exceptions in `com.trainer.profile`
    - Create `ProfileAlreadyExistsException` and `ProfileNotFoundException`
    - _Requirements: 1.2, 2.2, 3.2_

  - [ ] 6.2 Implement `AthleteProfileController` in `com.trainer.profile`
    - `POST /api/athlete-profile` → 201 with created profile
    - `GET /api/athlete-profile` → 200 with profile data
    - `PUT /api/athlete-profile` → 200 with updated profile
    - Extract authenticated user ID from `SecurityContext`
    - Use `@Valid` on request bodies; delegate to `AthleteProfileService`
    - _Requirements: 1.1, 1.15, 2.1, 2.6, 2.7, 3.1_

  - [ ] 6.3 Extend exception handling for profile-specific exceptions
    - Handle `ProfileAlreadyExistsException` → 409
    - Handle `ProfileNotFoundException` → 404
    - Handle validation errors with field-level detail
    - Ensure no stack traces or internal details are exposed
    - _Requirements: 1.2, 2.2, 3.2_

  - [ ]* 6.4 Write property tests for validation (`AthleteProfileValidationPropertyTest`)
    - **Property 2: Invalid input rejection** — for any payload violating exactly one validation rule, the API returns HTTP 400
    - **Validates: Requirements 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.11, 1.12, 1.13, 3.4**
    - **Property 8: Race time ordering validation** — for any pair of adjacent non-null race times where longer distance ≤ shorter distance, the API returns HTTP 400
    - **Validates: Requirements 7.1, 7.2, 7.3**
    - _Uses jqwik `@Property(tries = 100)` with MockMvc; arbitraries for invalid payloads_

- [ ] 7. Write backend integration tests
  - [ ]* 7.1 Write Spring Boot integration tests (`AthleteProfileIntegrationTest`)
    - Full create → get → update cycle against H2 in-memory database
    - Verify 409 on duplicate creation, 404 on missing profile
    - Verify zone calculation triggered on create/update with thresholds
    - Verify zone deletion when threshold removed
    - Verify authentication enforcement (401 without token)
    - Verify profile isolation (user A cannot access user B's profile)
    - _Requirements: 1.1, 1.2, 1.15, 2.1, 2.2, 2.6, 2.7, 3.1, 3.2, 3.5, 3.6, 3.7, 3.8_

- [ ] 8. Backend checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 9. Implement frontend service and models
  - [ ] 9.1 Create TypeScript interfaces in `src/app/core/profile/profile.model.ts`
    - Define `ProfileRequest`, `ProfileResponse`, `HrProfileResponse`, `HrZoneResponse`, `PaceProfileResponse`, `PaceZoneResponse`
    - _Requirements: 2.1, 2.3, 2.4_

  - [ ] 9.2 Create `AthleteProfileService` in `src/app/core/profile/`
    - Implement `getProfile()`, `createProfile(data)`, `updateProfile(data)` methods
    - Use `HttpClient` to call `/api/athlete-profile`
    - _Requirements: 1.1, 2.1, 3.1_

- [ ] 10. Implement `ProfilePageComponent`
  - [ ] 10.1 Create `ProfilePageComponent` in `src/app/features/athlete-profile/`
    - Standalone component with reactive form for all profile fields
    - Handle create mode (empty form, "Create Profile" button) vs edit mode (pre-populated, "Save Changes" button)
    - Use PrimeNG components: `p-datepicker`, `p-inputnumber`, `p-inputmask`, `p-button`, `p-table`, `p-toast`, `p-progressspinner`, `p-message`
    - Implement client-side validation: required fields, numeric ranges, maxHR > restingHR, lthr constraints, race time ordering
    - Display HR zones and Pace zones in read-only tables
    - Show "no zones" messages when HR/Pace profiles are absent
    - Handle loading state on initial page entry
    - Handle success/error notifications (toast auto-dismiss 5s for success, persistent for errors)
    - Implement MM:SS formatting for threshold pace and pace zone display
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9, 6.10, 6.11, 6.12, 6.13_

  - [ ]* 10.2 Write property tests for `ProfilePageComponent` (`profile-page.component.spec.ts`)
    - **Property 10: Client-side validation prevents API calls** — for any form state with at least one invalid field, submitting does not dispatch an HTTP request and shows inline errors
    - **Validates: Requirements 6.5**
    - **Property 11: Pace formatting round-trip** — for any integer in [150, 900], formatting as MM:SS and parsing back produces the original value
    - **Validates: Requirements 6.10**
    - _Uses fast-check `fc.integer({ min: 150, max: 900 })` and invalid form state generators_

- [ ] 11. Wire route configuration
  - [ ] 11.1 Add profile route to `app.routes.ts`
    - Add `{ path: 'profile', loadComponent: () => import('./features/athlete-profile/profile-page.component'), canActivate: [authGuard] }`
    - _Requirements: 6.1_

- [ ] 12. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Backend PBT uses jqwik (already in `pom.xml`); frontend PBT uses fast-check 3.22.0 (add to `package.json` devDependencies)
- Checkpoints ensure incremental validation at the zone calculator, backend, and frontend boundaries
- Zone calculators are pure functions — ideal targets for property-based testing
- The Flyway migration is V2 since V1 already exists for the users table
- `SecurityConfig` must be updated to require authentication on `/api/athlete-profile/**`

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2"] },
    { "id": 2, "tasks": ["1.3", "2.1", "2.3"] },
    { "id": 3, "tasks": ["2.2", "2.4", "4.1"] },
    { "id": 4, "tasks": ["4.2"] },
    { "id": 5, "tasks": ["5.1", "6.1"] },
    { "id": 6, "tasks": ["5.2", "6.2"] },
    { "id": 7, "tasks": ["6.3", "6.4"] },
    { "id": 8, "tasks": ["7.1"] },
    { "id": 9, "tasks": ["9.1"] },
    { "id": 10, "tasks": ["9.2"] },
    { "id": 11, "tasks": ["10.1"] },
    { "id": 12, "tasks": ["10.2", "11.1"] }
  ]
}
```
