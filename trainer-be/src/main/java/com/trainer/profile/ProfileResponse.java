package com.trainer.profile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO representing the full athlete profile including nested HR and Pace zone data.
 * When no HR or Pace profile exists, the corresponding field is null.
 */
public record ProfileResponse(
        Long id,
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
        Integer marathonSeconds,
        HrProfileResponse hrProfile,
        PaceProfileResponse paceProfile
) {
}
