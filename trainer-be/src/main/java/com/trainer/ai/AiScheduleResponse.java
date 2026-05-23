package com.trainer.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiScheduleResponse(
        Integer weekNumber,
        Integer dayOfWeek,
        Integer orderInDay
) {}
