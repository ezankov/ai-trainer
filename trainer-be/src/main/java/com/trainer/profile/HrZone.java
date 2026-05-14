package com.trainer.profile;

import jakarta.persistence.*;

/**
 * JPA entity mapped to the {@code trainer.hr_zones} table.
 * Represents a single heart rate training zone within an HR Profile.
 */
@Entity
@Table(name = "hr_zones", schema = "trainer")
public class HrZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hr_profile_id", nullable = false)
    private Long hrProfileId;

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

    public Long getHrProfileId() {
        return hrProfileId;
    }

    public void setHrProfileId(Long hrProfileId) {
        this.hrProfileId = hrProfileId;
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
