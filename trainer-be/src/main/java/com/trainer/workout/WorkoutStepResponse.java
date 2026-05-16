package com.trainer.workout;

import java.util.UUID;

/**
 * Response DTO for a single workout step, included in WorkoutResponse.
 */
public record WorkoutStepResponse(
        UUID id,
        Integer stepOrder,
        String stepName,
        String intensity,
        String durationType,
        Integer durationValue,
        String targetType,
        Integer targetValueLow,
        Integer targetValueHigh,
        String notes
) {
}
