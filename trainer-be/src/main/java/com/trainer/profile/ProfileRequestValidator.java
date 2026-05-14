package com.trainer.profile;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Cross-field validator for athlete profile requests.
 * <p>
 * This validator runs AFTER Bean Validation has passed, so individual field values
 * are already guaranteed to be within their valid ranges. It checks relationships
 * between fields that cannot be expressed with simple annotations.
 * <p>
 * Validates:
 * <ul>
 *   <li>maxHR > restingHR</li>
 *   <li>lthr > restingHR && lthr <= maxHR (when lthr is provided)</li>
 *   <li>dateOfBirth results in age ≥ 13 and is not in the future</li>
 *   <li>Race time ordering: 5K < 10K < Half < Marathon (only between non-null adjacent pairs)</li>
 * </ul>
 * All errors are collected and thrown together in a single {@link ProfileValidationException}.
 */
@Component
public class ProfileRequestValidator {

    /**
     * Validates cross-field constraints on a CreateProfileRequest.
     *
     * @param request the validated request DTO
     * @throws ProfileValidationException if any cross-field validation fails
     */
    public void validate(CreateProfileRequest request) {
        validate(
                request.dateOfBirth(),
                request.restingHR(),
                request.maxHR(),
                request.lthr(),
                request.fiveKSeconds(),
                request.tenKSeconds(),
                request.halfMarathonSeconds(),
                request.marathonSeconds()
        );
    }

    /**
     * Validates cross-field constraints on an UpdateProfileRequest.
     *
     * @param request the validated request DTO
     * @throws ProfileValidationException if any cross-field validation fails
     */
    public void validate(UpdateProfileRequest request) {
        validate(
                request.dateOfBirth(),
                request.restingHR(),
                request.maxHR(),
                request.lthr(),
                request.fiveKSeconds(),
                request.tenKSeconds(),
                request.halfMarathonSeconds(),
                request.marathonSeconds()
        );
    }

    private void validate(
            LocalDate dateOfBirth,
            Integer restingHR,
            Integer maxHR,
            Integer lthr,
            Integer fiveKSeconds,
            Integer tenKSeconds,
            Integer halfMarathonSeconds,
            Integer marathonSeconds
    ) {
        List<ProfileValidationException.FieldValidationError> errors = new ArrayList<>();

        validateHeartRates(restingHR, maxHR, lthr, errors);
        validateDateOfBirth(dateOfBirth, errors);
        validateRaceTimeOrdering(fiveKSeconds, tenKSeconds, halfMarathonSeconds, marathonSeconds, errors);

        if (!errors.isEmpty()) {
            throw new ProfileValidationException(errors);
        }
    }

    private void validateHeartRates(
            Integer restingHR,
            Integer maxHR,
            Integer lthr,
            List<ProfileValidationException.FieldValidationError> errors
    ) {
        if (restingHR != null && maxHR != null && maxHR <= restingHR) {
            errors.add(new ProfileValidationException.FieldValidationError(
                    "maxHR",
                    "Max heart rate must be greater than resting heart rate"
            ));
        }

        if (lthr != null && restingHR != null && lthr <= restingHR) {
            errors.add(new ProfileValidationException.FieldValidationError(
                    "lthr",
                    "LTHR must be greater than resting heart rate"
            ));
        }

        if (lthr != null && maxHR != null && lthr > maxHR) {
            errors.add(new ProfileValidationException.FieldValidationError(
                    "lthr",
                    "LTHR must be less than or equal to max heart rate"
            ));
        }
    }

    private void validateDateOfBirth(
            LocalDate dateOfBirth,
            List<ProfileValidationException.FieldValidationError> errors
    ) {
        if (dateOfBirth == null) {
            return;
        }

        LocalDate today = LocalDate.now();

        if (dateOfBirth.isAfter(today)) {
            errors.add(new ProfileValidationException.FieldValidationError(
                    "dateOfBirth",
                    "Date of birth must not be in the future"
            ));
            return; // No point checking age if date is in the future
        }

        int age = Period.between(dateOfBirth, today).getYears();
        if (age < 13) {
            errors.add(new ProfileValidationException.FieldValidationError(
                    "dateOfBirth",
                    "User must be at least 13 years old"
            ));
        }
    }

    private void validateRaceTimeOrdering(
            Integer fiveKSeconds,
            Integer tenKSeconds,
            Integer halfMarathonSeconds,
            Integer marathonSeconds,
            List<ProfileValidationException.FieldValidationError> errors
    ) {
        if (fiveKSeconds != null && tenKSeconds != null && tenKSeconds <= fiveKSeconds) {
            errors.add(new ProfileValidationException.FieldValidationError(
                    "tenKSeconds",
                    "10K time must be greater than 5K time"
            ));
        }

        if (tenKSeconds != null && halfMarathonSeconds != null && halfMarathonSeconds <= tenKSeconds) {
            errors.add(new ProfileValidationException.FieldValidationError(
                    "halfMarathonSeconds",
                    "Half-marathon time must be greater than 10K time"
            ));
        }

        if (halfMarathonSeconds != null && marathonSeconds != null && marathonSeconds <= halfMarathonSeconds) {
            errors.add(new ProfileValidationException.FieldValidationError(
                    "marathonSeconds",
                    "Marathon time must be greater than half-marathon time"
            ));
        }
    }
}
