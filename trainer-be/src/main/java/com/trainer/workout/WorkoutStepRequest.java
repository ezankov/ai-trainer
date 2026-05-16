package com.trainer.workout;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO representing a single workout step in a create/update request.
 * Fields use String types for enum values so the validator can parse and report
 * meaningful errors rather than relying solely on Bean Validation.
 */
public record WorkoutStepRequest(

        @Size(max = 50)
        String stepName,

        @NotNull
        String intensity,

        @NotNull
        String durationType,

        Integer durationValue,

        @NotNull
        String targetType,

        Integer targetValueLow,

        Integer targetValueHigh,

        @Size(max = 255)
        String notes
) {
}
