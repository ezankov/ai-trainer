package com.trainer.trainingplan;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a training plan.
 * Bean Validation annotations enforce per-field constraints.
 * Enum validation (distance, duration, aiModel) is handled in the service layer
 * to provide clear error messages for invalid values.
 */
@ValidLongRunDay
public record CreateTrainingPlanRequest(
        @NotBlank(message = "Event name is required")
        @Size(max = 100, message = "Event name must be at most 100 characters")
        String eventName,

        @NotNull(message = "Distance is required")
        String distance,

        @NotNull(message = "Duration is required")
        String duration,

        @NotNull(message = "Race date is required")
        @Future(message = "Race date must be in the future")
        LocalDate raceDate,

        @NotNull(message = "Target pace is required")
        @Min(value = 150, message = "Target pace must be at least 150 seconds per km")
        @Max(value = 900, message = "Target pace must be at most 900 seconds per km")
        Integer targetPaceSecondsPerKm,

        @NotNull(message = "AI model is required")
        String aiModel,

        @NotEmpty(message = "Training days must not be empty")
        @ValidTrainingDays
        List<Integer> trainingDays,

        @NotNull(message = "Long run day is required")
        @Min(value = 1, message = "Long run day must be between 1 (Monday) and 7 (Sunday)")
        @Max(value = 7, message = "Long run day must be between 1 (Monday) and 7 (Sunday)")
        Integer longRunDay
) {
}
