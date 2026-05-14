package com.trainer.profile;

/**
 * Response DTO representing a single pace training zone.
 */
public record PaceZoneResponse(
        int zoneNumber,
        String name,
        int lowerBound,
        int upperBound
) {
}
