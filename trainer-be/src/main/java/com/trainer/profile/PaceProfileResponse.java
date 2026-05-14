package com.trainer.profile;

import java.util.List;

/**
 * Response DTO representing the Pace profile with its training zones.
 */
public record PaceProfileResponse(
        List<PaceZoneResponse> zones
) {
}
