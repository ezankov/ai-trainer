package com.trainer.trainingplan;

import java.util.List;
import java.util.UUID;

/**
 * Summary DTO for a workout referenced within a plan workout entry.
 * Includes the full list of workout steps for detailed display.
 */
public record WorkoutSummaryResponse(
        UUID id,
        String name,
        String sportType,
        String subSport,
        Integer numValidSteps,
        List<WorkoutStepResponse> steps
) {
}
