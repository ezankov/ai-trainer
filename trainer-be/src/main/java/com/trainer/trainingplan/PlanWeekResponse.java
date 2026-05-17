package com.trainer.trainingplan;

import java.util.List;

/**
 * Response DTO for a single week within a training plan detail view.
 */
public record PlanWeekResponse(
        Integer weekNumber,
        List<PlanWorkoutEntryResponse> workouts
) {
}
