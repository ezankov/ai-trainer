package com.trainer.profile;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pure-function calculator that derives 6 pace training zones from a Threshold Pace value.
 *
 * <p>Calculation approach:
 * <ol>
 *   <li>Convert TP (seconds/km) to speed: {@code tpSpeed = 1000.0 / tp}</li>
 *   <li>Apply intensity percentages to get zone speed boundaries</li>
 *   <li>Convert back to pace: {@code pace = round(1000.0 / speed)}</li>
 * </ol>
 *
 * <p>Zone 1 upper bound is capped at 900 (slowest pace).
 * Zone 6 lower bound is capped at 150 (fastest pace).
 *
 * <p>Lower bound = faster pace (fewer seconds/km), upper bound = slower pace (more seconds/km).
 */
@Component
public class PaceZoneCalculator {

    /**
     * Calculates 6 pace zones from the given threshold pace.
     *
     * @param thresholdPaceSecondsPerKm threshold pace in seconds per kilometre (valid range: 150–900)
     * @return list of 6 PaceZone objects ordered by zone number (1–6), without id or paceProfileId set
     */
    public List<PaceZone> calculate(int thresholdPaceSecondsPerKm) {
        double tpSpeed = 1000.0 / thresholdPaceSecondsPerKm;

        // Zone boundaries defined by speed percentages of TP speed
        // Lower bound = faster pace (higher speed %), upper bound = slower pace (lower speed %)
        // All boundaries are clamped to [150, 900] range
        // After clamping, ensure lowerBound < upperBound for each zone
        int zone1Upper = 900; // cap: slowest displayable pace
        int zone1Lower = ensureValid(clamp(paceFromSpeed(tpSpeed * 0.72)), zone1Upper);

        int zone2Upper = clamp(paceFromSpeed(tpSpeed * 0.72));
        int zone2Lower = ensureValid(clamp(paceFromSpeed(tpSpeed * 0.87)), zone2Upper);

        int zone3Upper = clamp(paceFromSpeed(tpSpeed * 0.88));
        int zone3Lower = ensureValid(clamp(paceFromSpeed(tpSpeed * 0.93)), zone3Upper);

        int zone4Upper = clamp(paceFromSpeed(tpSpeed * 0.94));
        int zone4Lower = ensureValid(clamp(paceFromSpeed(tpSpeed * 1.02)), zone4Upper);

        int zone5Upper = clamp(paceFromSpeed(tpSpeed * 1.03));
        int zone5Lower = ensureValid(clamp(paceFromSpeed(tpSpeed * 1.11)), zone5Upper);

        int zone6Lower = 150; // cap: fastest displayable pace
        int zone6Upper = ensureUpperValid(clamp(paceFromSpeed(tpSpeed * 1.11)), zone6Lower);

        return List.of(
                createZone(1, "Recovery", zone1Lower, zone1Upper),
                createZone(2, "Aerobic Endurance", zone2Lower, zone2Upper),
                createZone(3, "Aerobic Power", zone3Lower, zone3Upper),
                createZone(4, "Threshold", zone4Lower, zone4Upper),
                createZone(5, "Anaerobic Endurance", zone5Lower, zone5Upper),
                createZone(6, "Anaerobic Power", zone6Lower, zone6Upper)
        );
    }

    private int paceFromSpeed(double speed) {
        return (int) Math.round(1000.0 / speed);
    }

    private int clamp(int pace) {
        return Math.max(150, Math.min(pace, 900));
    }

    /**
     * Ensures lowerBound < upperBound by capping lowerBound at upperBound - 1.
     */
    private int ensureValid(int lowerBound, int upperBound) {
        return Math.min(lowerBound, upperBound - 1);
    }

    /**
     * Ensures lowerBound < upperBound by raising upperBound to lowerBound + 1.
     */
    private int ensureUpperValid(int upperBound, int lowerBound) {
        return Math.max(upperBound, lowerBound + 1);
    }

    private PaceZone createZone(int zoneNumber, String name, int lowerBound, int upperBound) {
        PaceZone zone = new PaceZone();
        zone.setZoneNumber(zoneNumber);
        zone.setName(name);
        zone.setLowerBound(lowerBound);
        zone.setUpperBound(upperBound);
        return zone;
    }
}
