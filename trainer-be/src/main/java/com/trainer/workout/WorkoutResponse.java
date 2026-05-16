package com.trainer.workout;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full workout response DTO returned for single-workout endpoints (POST, GET by id, PUT).
 * Includes the ordered list of workout steps.
 */
public record WorkoutResponse(
        UUID id,
        String name,
        String sportType,
        String subSport,
        Integer numValidSteps,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<WorkoutStepResponse> steps
) {
}
