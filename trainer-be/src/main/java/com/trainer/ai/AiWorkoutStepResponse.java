package com.trainer.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiWorkoutStepResponse(
        Integer stepOrder,
        String stepName,
        String intensity,
        String durationType,
        Integer durationValue,
        String targetType,
        Integer targetValueLow,
        Integer targetValueHigh
) {}
