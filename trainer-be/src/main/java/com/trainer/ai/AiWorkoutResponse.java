package com.trainer.ai;

import java.util.List;

public record AiWorkoutResponse(
    String name,
    String sportType,
    String subSport,
    List<AiWorkoutStepResponse> steps,
    AiScheduleResponse schedule
) {}
