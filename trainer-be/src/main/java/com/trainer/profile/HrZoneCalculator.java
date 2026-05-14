package com.trainer.profile;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pure calculator that derives 6 heart rate training zones from resting HR, LTHR, and max HR.
 * <p>
 * Zone boundaries are calculated as percentages of LTHR. Adjacent zones share boundary values
 * (upper bound of zone N = lower bound of zone N+1).
 */
@Component
public class HrZoneCalculator {

    /**
     * Calculates 6 HR training zones based on LTHR percentage formula.
     *
     * @param restingHR the athlete's resting heart rate (bpm)
     * @param lthr      the athlete's lactate threshold heart rate (bpm)
     * @param maxHR     the athlete's maximum heart rate (bpm)
     * @return list of 6 HrZone objects (without id and hrProfileId set)
     */
    public List<HrZone> calculate(int restingHR, int lthr, int maxHR) {
        int zone1Upper = (int) Math.floor(lthr * 0.80);
        int zone2Upper = (int) Math.floor(lthr * 0.90);
        int zone3Upper = (int) Math.floor(lthr * 0.95);
        int zone4Upper = (int) Math.floor(lthr * 1.02);
        int zone5Upper = (int) Math.floor(lthr * 1.06);

        return List.of(
                createZone(1, "Recovery", restingHR, zone1Upper),
                createZone(2, "Aerobic Endurance", zone1Upper, zone2Upper),
                createZone(3, "Aerobic Power", zone2Upper, zone3Upper),
                createZone(4, "Threshold", zone3Upper, zone4Upper),
                createZone(5, "Anaerobic Endurance", zone4Upper, zone5Upper),
                createZone(6, "Anaerobic Power", zone5Upper, maxHR)
        );
    }

    private HrZone createZone(int zoneNumber, String name, int lowerBound, int upperBound) {
        HrZone zone = new HrZone();
        zone.setZoneNumber(zoneNumber);
        zone.setName(name);
        zone.setLowerBound(lowerBound);
        zone.setUpperBound(upperBound);
        return zone;
    }
}
