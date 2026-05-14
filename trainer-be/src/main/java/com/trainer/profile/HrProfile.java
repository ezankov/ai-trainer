package com.trainer.profile;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity mapped to the {@code trainer.hr_profiles} table.
 * Represents a set of 6 heart rate training zones derived from LTHR.
 */
@Entity
@Table(name = "hr_profiles", schema = "trainer")
public class HrProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "athlete_profile_id", nullable = false, unique = true)
    private Long athleteProfileId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "hrProfileId", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("zoneNumber ASC")
    private List<HrZone> zones = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Lifecycle callbacks
    // -------------------------------------------------------------------------

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAthleteProfileId() {
        return athleteProfileId;
    }

    public void setAthleteProfileId(Long athleteProfileId) {
        this.athleteProfileId = athleteProfileId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<HrZone> getZones() {
        return zones;
    }

    public void setZones(List<HrZone> zones) {
        this.zones = zones;
    }
}
