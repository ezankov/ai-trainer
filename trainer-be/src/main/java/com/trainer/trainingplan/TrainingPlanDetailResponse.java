package com.trainer.trainingplan;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for the plan detail endpoint (extends summary with workout weeks).
 */
public record TrainingPlanDetailResponse(
        UUID id,
        String eventName,
        String distance,
        String duration,
        LocalDate raceDate,
        Integer targetPaceSecondsPerKm,
        String aiModel,
        List<Integer> trainingDays,
        Integer longRunDay,
        String state,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<PlanWeekResponse> weeks
) {
}
