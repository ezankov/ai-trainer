package com.trainer.profile;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO for updating an athlete profile (full replacement).
 * Same structure and validation constraints as {@link CreateProfileRequest}.
 */
public record UpdateProfileRequest(
        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @NotNull(message = "Weight is required")
        @DecimalMin(value = "20.0", message = "Weight must be at least 20.0 kg")
        @DecimalMax(value = "300.0", message = "Weight must be at most 300.0 kg")
        BigDecimal weightKg,

        @NotNull(message = "Resting heart rate is required")
        @Min(value = 25, message = "Resting heart rate must be at least 25")
        @Max(value = 120, message = "Resting heart rate must be at most 120")
        Integer restingHR,

        @NotNull(message = "Max heart rate is required")
        @Min(value = 100, message = "Max heart rate must be at least 100")
        @Max(value = 250, message = "Max heart rate must be at most 250")
        Integer maxHR,

        @Min(value = 100, message = "LTHR must be at least 100")
        @Max(value = 250, message = "LTHR must be at most 250")
        Integer lthr,

        @Min(value = 150, message = "Threshold pace must be at least 150 seconds per km")
        @Max(value = 900, message = "Threshold pace must be at most 900 seconds per km")
        Integer thresholdPaceSecondsPerKm,

        @DecimalMin(value = "20.0", message = "VO2 Max must be at least 20.0")
        @DecimalMax(value = "90.0", message = "VO2 Max must be at most 90.0")
        BigDecimal vo2Max,

        @Min(value = 1, message = "5K time must be at least 1 second")
        @Max(value = 10800, message = "5K time must be at most 10800 seconds")
        Integer fiveKSeconds,

        @Min(value = 1, message = "10K time must be at least 1 second")
        @Max(value = 21600, message = "10K time must be at most 21600 seconds")
        Integer tenKSeconds,

        @Min(value = 1, message = "Half-marathon time must be at least 1 second")
        @Max(value = 43200, message = "Half-marathon time must be at most 43200 seconds")
        Integer halfMarathonSeconds,

        @Min(value = 1, message = "Marathon time must be at least 1 second")
        @Max(value = 86400, message = "Marathon time must be at most 86400 seconds")
        Integer marathonSeconds
) {
}
