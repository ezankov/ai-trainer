package com.trainer.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record AthleteProfileToolResponse(
    LocalDate dateOfBirth,
    BigDecimal weightKg,
    Integer restingHR,
    Integer maxHR,
    Integer lthr,
    Integer thresholdPaceSecondsPerKm,
    BigDecimal vo2Max,
    Integer fiveKSeconds,
    Integer tenKSeconds,
    Integer halfMarathonSeconds,
    Integer marathonSeconds
) {}
