package com.trainer.trainingplan;

/**
 * Response DTO for a single workout step within a plan workout entry.
 */
public record WorkoutStepResponse(
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
