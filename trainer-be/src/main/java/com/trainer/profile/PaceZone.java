package com.trainer.profile;

import jakarta.persistence.*;

/**
 * JPA entity mapped to the {@code trainer.pace_zones} table.
 * Represents a single pace training zone within a Pace Profile.
 */
@Entity
@Table(name = "pace_zones", schema = "trainer")
public class PaceZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pace_profile_id", nullable = false)
    private Long paceProfileId;

    @Column(name = "zone_number", nullable = false)
    private Integer zoneNumber;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "lower_bound", nullable = false)
    private Integer lowerBound;

    @Column(name = "upper_bound", nullable = false)
    private Integer upperBound;

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPaceProfileId() {
        return paceProfileId;
    }

    public void setPaceProfileId(Long paceProfileId) {
        this.paceProfileId = paceProfileId;
    }

    public Integer getZoneNumber() {
        return zoneNumber;
    }

    public void setZoneNumber(Integer zoneNumber) {
        this.zoneNumber = zoneNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(Integer lowerBound) {
        this.lowerBound = lowerBound;
    }

    public Integer getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(Integer upperBound) {
        this.upperBound = upperBound;
    }
}
