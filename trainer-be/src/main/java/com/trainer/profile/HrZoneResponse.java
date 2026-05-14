package com.trainer.profile;

/**
 * Response DTO representing a single heart rate training zone.
 */
public record HrZoneResponse(
        int zoneNumber,
        String name,
        int lowerBound,
        int upperBound
) {
}
