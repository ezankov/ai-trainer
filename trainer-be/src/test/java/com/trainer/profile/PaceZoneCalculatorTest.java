package com.trainer.profile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PaceZoneCalculator}.
 * Verifies zone calculation against the design document formula.
 */
class PaceZoneCalculatorTest {

    private PaceZoneCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PaceZoneCalculator();
    }

    @Test
    void calculate_withDesignExample_producesExpectedZones() {
        // Given: TP = 270 seconds/km (4:30/km)
        int tp = 270;
        // tpSpeed = 1000.0 / 270 ≈ 3.7037 m/s

        // When
        List<PaceZone> zones = calculator.calculate(tp);

        // Then: exactly 6 zones
        assertThat(zones).hasSize(6);

        // Zone 1: Recovery — pace at 72% speed to 900 cap
        // round(1000 / (3.7037 * 0.72)) = round(1000 / 2.6667) = round(375.0) = 375
        assertZone(zones.get(0), 1, "Recovery", 375, 900);

        // Zone 2: Aerobic Endurance — pace at 87% speed to pace at 72% speed
        // round(1000 / (3.7037 * 0.87)) = round(1000 / 3.2222) = round(310.3) = 310
        assertZone(zones.get(1), 2, "Aerobic Endurance", 310, 375);

        // Zone 3: Aerobic Power — pace at 93% speed to pace at 88% speed
        // round(1000 / (3.7037 * 0.93)) = round(1000 / 3.4444) = round(290.3) = 290
        // round(1000 / (3.7037 * 0.88)) = round(1000 / 3.2593) = round(306.8) = 307
        assertZone(zones.get(2), 3, "Aerobic Power", 290, 307);

        // Zone 4: Threshold — pace at 102% speed to pace at 94% speed
        // round(1000 / (3.7037 * 1.02)) = round(1000 / 3.7778) = round(264.7) = 265
        // round(1000 / (3.7037 * 0.94)) = round(1000 / 3.4815) = round(287.2) = 287
        assertZone(zones.get(3), 4, "Threshold", 265, 287);

        // Zone 5: Anaerobic Endurance — pace at 111% speed to pace at 103% speed
        // round(1000 / (3.7037 * 1.11)) = round(1000 / 4.1111) = round(243.2) = 243
        // round(1000 / (3.7037 * 1.03)) = round(1000 / 3.8148) = round(262.1) = 262
        assertZone(zones.get(4), 5, "Anaerobic Endurance", 243, 262);

        // Zone 6: Anaerobic Power — 150 cap to pace at 111% speed
        assertZone(zones.get(5), 6, "Anaerobic Power", 150, 243);
    }

    @Test
    void calculate_zone1UpperBoundCappedAt900() {
        // Even with a very slow TP, zone 1 upper bound should be 900
        List<PaceZone> zones = calculator.calculate(900);

        assertThat(zones.get(0).getUpperBound()).isEqualTo(900);
    }

    @Test
    void calculate_zone6LowerBoundCappedAt150() {
        // Even with a very fast TP, zone 6 lower bound should be 150
        List<PaceZone> zones = calculator.calculate(150);

        assertThat(zones.get(5).getLowerBound()).isEqualTo(150);
    }

    @Test
    void calculate_allZonesHaveLowerBoundLessThanUpperBound() {
        // For a mid-range TP value
        List<PaceZone> zones = calculator.calculate(300);

        for (PaceZone zone : zones) {
            assertThat(zone.getLowerBound())
                    .as("Zone %d lower bound should be less than upper bound", zone.getZoneNumber())
                    .isLessThan(zone.getUpperBound());
        }
    }

    @Test
    void calculate_doesNotSetIdOrPaceProfileId() {
        List<PaceZone> zones = calculator.calculate(270);

        for (PaceZone zone : zones) {
            assertThat(zone.getId()).isNull();
            assertThat(zone.getPaceProfileId()).isNull();
        }
    }

    @Test
    void calculate_withFastTP_producesValidZones() {
        // TP = 150 (2:30/km — very fast)
        List<PaceZone> zones = calculator.calculate(150);

        assertThat(zones).hasSize(6);
        // All zones should have lowerBound < upperBound
        for (PaceZone zone : zones) {
            assertThat(zone.getLowerBound())
                    .as("Zone %d lower bound should be less than upper bound", zone.getZoneNumber())
                    .isLessThan(zone.getUpperBound());
        }
        // Zone 6 lower bound capped at 150
        assertThat(zones.get(5).getLowerBound()).isEqualTo(150);
        // Zone 6 upper bound must be > 150 to maintain ordering
        assertThat(zones.get(5).getUpperBound()).isGreaterThan(150);
    }

    @Test
    void calculate_withSlowTP_producesValidZones() {
        // TP = 900 (15:00/km — very slow)
        List<PaceZone> zones = calculator.calculate(900);

        assertThat(zones).hasSize(6);
        // All zones should have lowerBound < upperBound
        for (PaceZone zone : zones) {
            assertThat(zone.getLowerBound())
                    .as("Zone %d lower bound should be less than upper bound", zone.getZoneNumber())
                    .isLessThan(zone.getUpperBound());
        }
        // Zone 1 upper bound capped at 900
        assertThat(zones.get(0).getUpperBound()).isEqualTo(900);
    }

    @Test
    void calculate_producesCorrectZoneNames() {
        List<PaceZone> zones = calculator.calculate(300);

        assertThat(zones.get(0).getName()).isEqualTo("Recovery");
        assertThat(zones.get(1).getName()).isEqualTo("Aerobic Endurance");
        assertThat(zones.get(2).getName()).isEqualTo("Aerobic Power");
        assertThat(zones.get(3).getName()).isEqualTo("Threshold");
        assertThat(zones.get(4).getName()).isEqualTo("Anaerobic Endurance");
        assertThat(zones.get(5).getName()).isEqualTo("Anaerobic Power");
    }

    @Test
    void calculate_producesCorrectZoneNumbers() {
        List<PaceZone> zones = calculator.calculate(300);

        for (int i = 0; i < 6; i++) {
            assertThat(zones.get(i).getZoneNumber()).isEqualTo(i + 1);
        }
    }

    private void assertZone(PaceZone zone, int expectedNumber, String expectedName,
                            int expectedLower, int expectedUpper) {
        assertThat(zone.getZoneNumber()).isEqualTo(expectedNumber);
        assertThat(zone.getName()).isEqualTo(expectedName);
        assertThat(zone.getLowerBound()).isEqualTo(expectedLower);
        assertThat(zone.getUpperBound()).isEqualTo(expectedUpper);
    }
}
