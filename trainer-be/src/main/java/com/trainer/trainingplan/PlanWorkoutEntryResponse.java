package com.trainer.trainingplan;

/**
 * Response DTO for a single workout entry within a plan week.
 */
public record PlanWorkoutEntryResponse(
        Integer dayOfWeek,
        Integer orderInDay,
        WorkoutSummaryResponse workout
) {
}
