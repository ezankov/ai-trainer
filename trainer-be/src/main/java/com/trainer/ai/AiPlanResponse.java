package com.trainer.ai;

import java.util.List;

public record AiPlanResponse(
    List<AiWorkoutResponse> workouts
) {}
