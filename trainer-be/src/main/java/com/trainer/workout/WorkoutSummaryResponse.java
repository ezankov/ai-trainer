package com.trainer.workout;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Summary workout response DTO returned for the list endpoint (GET /api/workouts).
 * Excludes the steps array for a lighter payload.
 */
public record WorkoutSummaryResponse(
        UUID id,
        String name,
        String sportType,
        String subSport,
        Integer numValidSteps,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
