package com.trainer.profile;

import java.util.List;

/**
 * Response DTO representing the HR profile with its training zones.
 */
public record HrProfileResponse(
        List<HrZoneResponse> zones
) {
}
