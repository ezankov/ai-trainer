package com.trainer.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HrZoneCalculator}.
 * Verifies zone calculation against the known example from the design document.
 */
class HrZoneCalculatorTest {

    private HrZoneCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new HrZoneCalculator();
    }

    @Test
    void calculate_withDesignExample_producesExpectedZones() {
        // Given: the example from the design document
        int restingHR = 48;
        int lthr = 168;
        int maxHR = 190;

        // When
        List<HrZone> zones = calculator.calculate(restingHR, lthr, maxHR);

        // Then: exactly 6 zones
        assertThat(zones).hasSize(6);

        // Zone 1: Recovery — 48 to 134
        assertZone(zones.get(0), 1, "Recovery", 48, 134);
        // Zone 2: Aerobic Endurance — 134 to 151
        assertZone(zones.get(1), 2, "Aerobic Endurance", 134, 151);
        // Zone 3: Aerobic Power — 151 to 159
        assertZone(zones.get(2), 3, "Aerobic Power", 151, 159);
        // Zone 4: Threshold — 159 to 171
        assertZone(zones.get(3), 4, "Threshold", 159, 171);
        // Zone 5: Anaerobic Endurance — 171 to 178
        assertZone(zones.get(4), 5, "Anaerobic Endurance", 171, 178);
        // Zone 6: Anaerobic Power — 178 to 190
        assertZone(zones.get(5), 6, "Anaerobic Power", 178, 190);
    }

    @Test
    void calculate_adjacentZonesShareBoundaryValues() {
        List<HrZone> zones = calculator.calculate(50, 170, 195);

        for (int i = 0; i < zones.size() - 1; i++) {
            assertThat(zones.get(i).getUpperBound())
                    .as("Upper bound of zone %d should equal lower bound of zone %d", i + 1, i + 2)
                    .isEqualTo(zones.get(i + 1).getLowerBound());
        }
    }

    @Test
    void calculate_zone1LowerBoundIsRestingHR() {
        int restingHR = 55;
        List<HrZone> zones = calculator.calculate(restingHR, 165, 200);

        assertThat(zones.get(0).getLowerBound()).isEqualTo(restingHR);
    }

    @Test
    void calculate_zone6UpperBoundIsMaxHR() {
        int maxHR = 205;
        List<HrZone> zones = calculator.calculate(45, 175, maxHR);

        assertThat(zones.get(5).getUpperBound()).isEqualTo(maxHR);
    }

    @Test
    void calculate_doesNotSetIdOrHrProfileId() {
        List<HrZone> zones = calculator.calculate(50, 160, 190);

        for (HrZone zone : zones) {
            assertThat(zone.getId()).isNull();
            assertThat(zone.getHrProfileId()).isNull();
        }
    }

    @Test
    void calculate_withLowLthr_producesValidZones() {
        // LTHR at minimum valid value (100), restingHR just below
        List<HrZone> zones = calculator.calculate(25, 100, 180);

        assertThat(zones).hasSize(6);
        // floor(100 * 0.80) = 80
        assertZone(zones.get(0), 1, "Recovery", 25, 80);
        // floor(100 * 0.90) = 90
        assertZone(zones.get(1), 2, "Aerobic Endurance", 80, 90);
        // floor(100 * 0.95) = 95
        assertZone(zones.get(2), 3, "Aerobic Power", 90, 95);
        // floor(100 * 1.02) = 102
        assertZone(zones.get(3), 4, "Threshold", 95, 102);
        // floor(100 * 1.06) = 106
        assertZone(zones.get(4), 5, "Anaerobic Endurance", 102, 106);
        // Zone 6: 106 to maxHR
        assertZone(zones.get(5), 6, "Anaerobic Power", 106, 180);
    }

    private void assertZone(HrZone zone, int expectedNumber, String expectedName,
                            int expectedLower, int expectedUpper) {
        assertThat(zone.getZoneNumber()).isEqualTo(expectedNumber);
        assertThat(zone.getName()).isEqualTo(expectedName);
        assertThat(zone.getLowerBound()).isEqualTo(expectedLower);
        assertThat(zone.getUpperBound()).isEqualTo(expectedUpper);
    }
}
