package com.trainer.trainingplan;

import java.util.UUID;

/**
 * Summary DTO for a workout referenced within a plan workout entry.
 */
public record WorkoutSummaryResponse(
        UUID id,
        String name,
        String sportType,
        String subSport,
        Integer numValidSteps
) {
}
