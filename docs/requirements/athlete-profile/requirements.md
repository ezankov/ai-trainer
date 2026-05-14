# Requirements Document

## Introduction

This feature introduces the Athlete Profile — the central entity that stores a runner's physiological metrics and race performance data. The profile is owned by the authenticated user and serves as the foundation for generating AI-based running plans. It includes core biometric attributes (date of birth, weight, resting and max heart rate), threshold values (LTHR and Threshold Pace), an optional VO2 Max measurement, personal best race times (5K, 10K, Half-Marathon, Marathon), and two derived sub-profiles: an HR Profile (6 heart rate training zones based on LTHR percentages) and a Pace Profile (6 pace-based training zones based on Threshold Pace speed percentages). Each user has exactly one Athlete Profile, which in turn has at most one HR Profile and at most one Pace Profile.

## Glossary

- **Athlete_Profile_API**: The Spring Boot REST controller handling endpoints under `/api/athlete-profile` for managing the athlete profile resource.
- **Athlete_Profile_Service**: The backend service layer responsible for business logic related to creating, reading, and updating the Athlete Profile.
- **Athlete_Profile**: The JPA entity representing the runner's core biometric data and race times, stored in the `trainer.athlete_profiles` table. Has a one-to-one relationship with a User.
- **HR_Profile**: The JPA entity representing heart rate training zones derived from the Athlete Profile's LTHR value. Stored in the `trainer.hr_profiles` table with a one-to-one relationship to the Athlete Profile. Contains 6 zones calculated as percentages of LTHR.
- **Pace_Profile**: The JPA entity representing pace-based training zones derived from the Athlete Profile's Threshold Pace value. Stored in the `trainer.pace_profiles` table with a one-to-one relationship to the Athlete Profile. Contains 6 zones calculated as speed percentages of TP.
- **LTHR**: Lactate Threshold Heart Rate — the heart rate at which lactate begins to accumulate in the blood faster than it can be cleared. Stored as an integer (beats per minute) on the Athlete Profile and used as the anchor for HR zone calculation.
- **TP**: Threshold Pace — the pace a runner can sustain at lactate threshold, stored as an integer in seconds per kilometre on the Athlete Profile and used as the anchor for pace zone calculation.
- **HR_Zone**: A named heart rate range (one of six zones) defined by a lower and upper beats-per-minute boundary, calculated as a percentage of LTHR.
- **Pace_Zone**: A named pace range (one of six zones) defined by a lower and upper pace in seconds per kilometre, derived from percentage thresholds applied to TP converted to speed (m/s).
- **VO2_Max**: Maximum rate of oxygen consumption measured during incremental exercise, expressed in millilitres of oxygen per kilogram of body weight per minute (ml/kg/min). Stored as a decimal with one decimal place on the Athlete Profile. Used as an input for AI plan generation but does not trigger any zone calculation.
- **Race_Time**: A duration representing the athlete's personal best for a specific race distance (5K, 10K, Half-Marathon, Marathon), stored as total seconds.
- **Profile_Page**: The Angular standalone component that displays and allows editing of the Athlete Profile, HR Profile, and Pace Profile.
- **User**: A person with a record in the `trainer.users` table who owns exactly one Athlete Profile.

---

## Requirements

### Requirement 1: Create Athlete Profile

**User Story:** As a logged-in user, I want to create my athlete profile with my biometric data and race times, so that the application can generate a personalised running plan.

#### Acceptance Criteria

1. WHEN a POST request is sent to `/api/athlete-profile` with valid biometric data and the authenticated User does not yet have an Athlete Profile, THE Athlete_Profile_API SHALL create a new Athlete_Profile record associated with the authenticated User and return HTTP 201 with the created profile in the response body, including all stored fields: `dateOfBirth`, `weightKg`, `restingHR`, `maxHR`, `lthr`, `thresholdPaceSecondsPerKm`, `vo2Max`, `fiveKSeconds`, `tenKSeconds`, `halfMarathonSeconds`, `marathonSeconds`, and the profile's unique identifier.
2. WHEN a POST request is sent to `/api/athlete-profile` and the authenticated User already has an Athlete Profile, THE Athlete_Profile_API SHALL return HTTP 409 with an error message indicating a profile already exists.
3. WHEN a POST request is sent to `/api/athlete-profile` with a `dateOfBirth` that is in the future or results in an age younger than 13 years relative to the current server date, THE Athlete_Profile_API SHALL return HTTP 400 with a validation error message.
4. WHEN a POST request is sent to `/api/athlete-profile` with a `weightKg` value less than 20.0 or greater than 300.0, THE Athlete_Profile_API SHALL return HTTP 400 with a validation error message. The `weightKg` field SHALL accept up to one decimal place of precision.
5. WHEN a POST request is sent to `/api/athlete-profile` with a `restingHR` value less than 25 or greater than 120, THE Athlete_Profile_API SHALL return HTTP 400 with a validation error message.
6. WHEN a POST request is sent to `/api/athlete-profile` with a `maxHR` value less than 100 or greater than 250, THE Athlete_Profile_API SHALL return HTTP 400 with a validation error message.
7. WHEN a POST request is sent to `/api/athlete-profile` with a `maxHR` value that is less than or equal to the provided `restingHR` value, THE Athlete_Profile_API SHALL return HTTP 400 with a validation error message indicating max heart rate must exceed resting heart rate.
8. WHEN a POST request is sent to `/api/athlete-profile` with any race time value that is less than or equal to zero or greater than 86400 (24 hours in seconds), THE Athlete_Profile_API SHALL return HTTP 400 with a validation error message.
9. WHEN a POST request is sent to `/api/athlete-profile` with a missing `dateOfBirth`, `weightKg`, `restingHR`, or `maxHR` field, THE Athlete_Profile_API SHALL return HTTP 400 with a validation error message identifying the missing field.
10. THE Athlete_Profile_API SHALL accept race time fields (`fiveKSeconds`, `tenKSeconds`, `halfMarathonSeconds`, `marathonSeconds`) and `vo2Max` as optional; missing race times and `vo2Max` SHALL be stored as null.
11. THE Athlete_Profile_API SHALL accept `lthr` as an optional integer field with valid range 100–250. WHEN provided and outside this range, THE Athlete_Profile_API SHALL return HTTP 400 with a validation error message.
12. THE Athlete_Profile_API SHALL accept `thresholdPaceSecondsPerKm` as an optional integer field with valid range 150–900 (representing paces from 2:30/km to 15:00/km). WHEN provided and outside this range, THE Athlete_Profile_API SHALL return HTTP 400 with a validation error message.
13. WHEN `lthr` is provided, THE Athlete_Profile_API SHALL validate that `lthr` is greater than `restingHR` and less than or equal to `maxHR`. IF this constraint is violated, THE Athlete_Profile_API SHALL return HTTP 400 with a validation error message.
14. THE Athlete_Profile_API SHALL accept `vo2Max` as an optional decimal field with up to one decimal place of precision. WHEN provided and outside the range 20.0–90.0, THE Athlete_Profile_API SHALL return HTTP 400 with a validation error message.
15. IF a POST request is sent to `/api/athlete-profile` without a valid JWT token or with an expired token, THEN THE Athlete_Profile_API SHALL return HTTP 401 with an error message indicating authentication is required.

---

### Requirement 2: Retrieve Athlete Profile

**User Story:** As a logged-in user, I want to view my athlete profile, so that I can review my current biometric data and race times.

#### Acceptance Criteria

1. WHEN a GET request is sent to `/api/athlete-profile` by an authenticated User who has an Athlete Profile, THE Athlete_Profile_API SHALL return HTTP 200 with the Athlete_Profile data including `dateOfBirth`, `weightKg`, `restingHR`, `maxHR`, `lthr`, `thresholdPaceSecondsPerKm`, `vo2Max`, `fiveKSeconds`, `tenKSeconds`, `halfMarathonSeconds`, and `marathonSeconds` fields.
2. WHEN a GET request is sent to `/api/athlete-profile` by an authenticated User who does not have an Athlete Profile, THE Athlete_Profile_API SHALL return HTTP 404 with an error message indicating no profile exists.
3. IF the Athlete_Profile has an associated HR_Profile, THEN THE Athlete_Profile_API SHALL include the HR_Profile data (zone number, name, lower bound, and upper bound for each HR_Zone) in the GET response.
4. IF the Athlete_Profile has an associated Pace_Profile, THEN THE Athlete_Profile_API SHALL include the Pace_Profile data (zone name, lower bound, and upper bound in seconds per kilometre for each Pace_Zone) in the GET response.
5. IF the Athlete_Profile does not have an associated HR_Profile or Pace_Profile, THEN THE Athlete_Profile_API SHALL return null for the missing sub-profile in the response.
6. IF a GET request is sent to `/api/athlete-profile` without a valid authentication token, THEN THE Athlete_Profile_API SHALL return HTTP 401.
7. THE Athlete_Profile_API SHALL only return the Athlete Profile belonging to the authenticated User; a User SHALL NOT be able to access another User's profile.

---

### Requirement 3: Update Athlete Profile

**User Story:** As a logged-in user, I want to update my athlete profile, so that my running plan reflects my current fitness level.

#### Acceptance Criteria

1. WHEN a PUT request is sent to `/api/athlete-profile` with valid updated biometric data by an authenticated User who has an Athlete Profile, THE Athlete_Profile_API SHALL update the existing Athlete_Profile record and return HTTP 200 with the full updated profile including associated HR_Profile and Pace_Profile in the response body.
2. WHEN a PUT request is sent to `/api/athlete-profile` by an authenticated User who does not have an Athlete Profile, THE Athlete_Profile_API SHALL return HTTP 404 with an error message indicating no profile exists to update.
3. THE Athlete_Profile_API SHALL treat PUT as a full replacement: all required fields (`dateOfBirth`, `weightKg`, `restingHR`, `maxHR`) must be present in the request body, and any optional field (including `vo2Max`) omitted from the request SHALL be stored as null.
4. THE Athlete_Profile_API SHALL apply the same validation rules for PUT requests as defined in Requirement 1 (acceptance criteria 3 through 10) and the cross-field race time validation defined in Requirement 7.
5. WHEN the `lthr` value is changed in a PUT request and is non-null, THE Athlete_Profile_Service SHALL trigger a complete recalculation of the HR_Profile zones.
6. WHEN the `thresholdPaceSecondsPerKm` value is changed in a PUT request and is non-null, THE Athlete_Profile_Service SHALL trigger a complete recalculation of the Pace_Profile zones.
7. WHEN a PUT request results in `lthr` being set to null, THE Athlete_Profile_Service SHALL delete the existing HR_Profile.
8. WHEN a PUT request results in `thresholdPaceSecondsPerKm` being set to null, THE Athlete_Profile_Service SHALL delete the existing Pace_Profile.

---

### Requirement 4: HR Profile Calculation

**User Story:** As a logged-in user, I want my heart rate training zones to be automatically calculated from my Lactate Threshold Heart Rate, so that I have accurate zone targets for my workouts.

#### Acceptance Criteria

1. WHEN an Athlete_Profile is created or updated with a valid `lthr` value, THE Athlete_Profile_Service SHALL generate an HR_Profile containing six HR_Zones using percentages of LTHR.
2. THE Athlete_Profile_Service SHALL calculate HR_Zones with the following percentage ranges of LTHR: Zone 1 "Recovery" (< 80% of LTHR), Zone 2 "Aerobic Endurance" (80%–90% of LTHR), Zone 3 "Aerobic Power" (91%–95% of LTHR), Zone 4 "Threshold" (96%–102% of LTHR), Zone 5 "Anaerobic Endurance" (103%–106% of LTHR), Zone 6 "Anaerobic Power" (> 106% of LTHR).
3. THE Athlete_Profile_Service SHALL store each HR_Zone with a zone number (1–6), a descriptive name, a lower bound in beats per minute, and an upper bound in beats per minute.
4. THE Athlete_Profile_Service SHALL round all HR_Zone boundary values to the nearest integer.
5. THE Athlete_Profile_Service SHALL calculate zone boundaries such that adjacent zones share their boundary value — the upper bound of Zone N SHALL equal the lower bound of Zone N+1.
6. FOR Zone 1 ("Recovery"), THE Athlete_Profile_Service SHALL use `restingHR` as the lower bound and floor(LTHR × 0.80) as the upper bound. FOR Zone 6 ("Anaerobic Power"), THE Athlete_Profile_Service SHALL use ceil(LTHR × 1.06) + 1 as the lower bound and `maxHR` as the upper bound.
7. WHEN the `lthr` value of an Athlete_Profile is updated, THE Athlete_Profile_Service SHALL replace the existing HR_Profile zones with newly calculated values.
8. WHEN an Athlete_Profile is created without an `lthr` value, THE Athlete_Profile_Service SHALL NOT generate an HR_Profile.

---

### Requirement 5: Pace Profile Calculation

**User Story:** As a logged-in user, I want my pace training zones to be automatically calculated from my Threshold Pace, so that I have accurate pace targets for different workout types.

#### Acceptance Criteria

1. WHEN an Athlete_Profile is created or updated with a valid `thresholdPaceSecondsPerKm` value, THE Athlete_Profile_Service SHALL generate a Pace_Profile containing six Pace_Zones derived from percentage thresholds applied to the Threshold Pace.
2. THE Athlete_Profile_Service SHALL convert `thresholdPaceSecondsPerKm` to speed in metres per second (1000 / pace) before applying intensity percentages, then convert the resulting speed boundaries back to seconds per kilometre. This ensures the inverse relationship between pace and intensity is correctly maintained.
3. THE Athlete_Profile_Service SHALL calculate Pace_Zones with the following intensity percentage ranges applied to Threshold Pace speed: Zone 1 "Recovery" (< 72% of TP speed, i.e., pace > 138% of TP), Zone 2 "Aerobic Endurance" (72%–87% of TP speed, i.e., pace 115%–138% of TP), Zone 3 "Aerobic Power" (88%–93% of TP speed, i.e., pace 107%–114% of TP), Zone 4 "Threshold" (94%–102% of TP speed, i.e., pace 98%–106% of TP), Zone 5 "Anaerobic Endurance" (103%–111% of TP speed, i.e., pace 90%–97% of TP), Zone 6 "Anaerobic Power" (> 111% of TP speed, i.e., pace < 90% of TP).
4. THE Athlete_Profile_Service SHALL store each Pace_Zone with a zone number (1–6), a zone name, a lower bound in seconds per kilometre, and an upper bound in seconds per kilometre, with all boundary values rounded to the nearest integer.
5. THE Athlete_Profile_Service SHALL order pace zone bounds such that the lower bound represents the faster pace (fewer seconds per km) and the upper bound represents the slower pace (more seconds per km) for each zone.
6. WHEN the `thresholdPaceSecondsPerKm` value of an Athlete_Profile is updated, THE Athlete_Profile_Service SHALL replace the existing Pace_Profile zones with newly calculated values.
7. WHEN an Athlete_Profile is created without a `thresholdPaceSecondsPerKm` value, THE Athlete_Profile_Service SHALL NOT generate a Pace_Profile.
8. WHEN the `thresholdPaceSecondsPerKm` value is removed (set to null) in an update, THE Athlete_Profile_Service SHALL delete the existing Pace_Profile.

---

### Requirement 6: Profile Page UI

**User Story:** As a logged-in user, I want a profile page where I can view and edit my athlete profile, so that I can manage my training data in one place.

#### Acceptance Criteria

1. THE Profile_Page SHALL display input fields for `dateOfBirth` (date picker restricted to dates that result in an age of at least 13 years and not in the future), `weightKg` (decimal input accepting values between 20.0 and 300.0 with up to 1 decimal place), `restingHR` (integer input accepting values between 25 and 120), `maxHR` (integer input accepting values between 100 and 250), `lthr` (optional integer input accepting values between 100 and 250), `thresholdPaceSecondsPerKm` (optional pace input displayed as `MM:SS /km` accepting values between 150 and 900 seconds), and `vo2Max` (optional decimal input accepting values between 20.0 and 90.0 with up to 1 decimal place, labelled "VO₂ Max (ml/kg/min)").
2. THE Profile_Page SHALL display input fields for race times: 5K, 10K, Half-Marathon, and Marathon, each accepting a duration in the format `HH:MM:SS`. Race time fields SHALL be optional.
3. WHEN the user has no existing Athlete Profile, THE Profile_Page SHALL display the form in creation mode with empty fields and a "Create Profile" submit button.
4. WHEN the user has an existing Athlete Profile, THE Profile_Page SHALL display the form pre-populated with current values and a "Save Changes" submit button.
5. WHEN the user submits the form with invalid data, THE Profile_Page SHALL display inline validation errors for each invalid field without sending a request to the Athlete_Profile_API. Client-side validation SHALL enforce: required fields (`dateOfBirth`, `weightKg`, `restingHR`, `maxHR`) are present, numeric values are within their specified bounds, `maxHR` exceeds `restingHR`, `lthr` is between `restingHR` and `maxHR` when provided, `vo2Max` is between 20.0 and 90.0 when provided, race times are greater than zero when provided, and longer race distances have greater times than shorter distances when both are provided.
6. WHILE a create or update request is in progress, THE Profile_Page SHALL disable the submit button and display a loading indicator.
7. WHEN the Athlete_Profile_API returns a successful response, THE Profile_Page SHALL display a success notification that auto-dismisses after 5 seconds and update the displayed data.
8. WHEN the Athlete_Profile_API returns an error response, THE Profile_Page SHALL display an error notification with the error message that remains visible until the user dismisses it.
9. THE Profile_Page SHALL display the calculated HR_Profile zones in a read-only table showing zone number, name, and heart rate range.
10. THE Profile_Page SHALL display the calculated Pace_Profile zones in a read-only table showing zone name and pace range formatted as `MM:SS /km`.
11. WHEN no Pace_Profile exists (no threshold pace provided), THE Profile_Page SHALL display a message indicating that pace zones will be calculated once a threshold pace is entered.
12. WHEN no HR_Profile exists (no LTHR provided), THE Profile_Page SHALL display a message indicating that HR zones will be calculated once an LTHR value is entered.
13. WHILE the Profile_Page is loading the Athlete Profile from the Athlete_Profile_API on initial page entry, THE Profile_Page SHALL display a loading indicator and hide the form until the request completes or fails.

---

### Requirement 7: Race Time Validation

**User Story:** As a logged-in user, I want the system to validate that my race times are logically consistent, so that calculation errors from impossible data are prevented.

#### Acceptance Criteria

1. WHEN a race time is provided for 10K that is less than or equal to the provided 5K time, THE Athlete_Profile_API SHALL return HTTP 400 with a validation error message indicating the 10K time must be greater than the 5K time.
2. WHEN a race time is provided for Half-Marathon that is less than or equal to the provided 10K time, THE Athlete_Profile_API SHALL return HTTP 400 with a validation error message indicating the Half-Marathon time must be greater than the 10K time.
3. WHEN a race time is provided for Marathon that is less than or equal to the provided Half-Marathon time, THE Athlete_Profile_API SHALL return HTTP 400 with a validation error message indicating the Marathon time must be greater than the Half-Marathon time.
4. THE Athlete_Profile_API SHALL only apply cross-field race time validation between pairs of race times that are both non-null.
5. IF multiple adjacent race time pairs fail validation in the same request, THEN THE Athlete_Profile_API SHALL return HTTP 400 with a validation error message for each failing pair, so that the user can correct all issues at once.
6. THE Athlete_Profile_API SHALL enforce the following upper bounds on individual race times: 5K at most 10800 seconds (3 hours), 10K at most 21600 seconds (6 hours), Half-Marathon at most 43200 seconds (12 hours), and Marathon at most 86400 seconds (24 hours).
