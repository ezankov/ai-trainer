package com.trainer.profile;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * JPA entity mapped to the {@code trainer.athlete_profiles} table.
 * Stores a runner's core biometric data and race times.
 */
@Entity
@Table(name = "athlete_profiles", schema = "trainer")
public class AthleteProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "weight_kg", nullable = false, precision = 4, scale = 1)
    private BigDecimal weightKg;

    @Column(name = "resting_hr", nullable = false)
    private Integer restingHR;

    @Column(name = "max_hr", nullable = false)
    private Integer maxHR;

    @Column(name = "lthr")
    private Integer lthr;

    @Column(name = "threshold_pace_seconds_per_km")
    private Integer thresholdPaceSecondsPerKm;

    @Column(name = "vo2_max", precision = 3, scale = 1)
    private BigDecimal vo2Max;

    @Column(name = "five_k_seconds")
    private Integer fiveKSeconds;

    @Column(name = "ten_k_seconds")
    private Integer tenKSeconds;

    @Column(name = "half_marathon_seconds")
    private Integer halfMarathonSeconds;

    @Column(name = "marathon_seconds")
    private Integer marathonSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // -------------------------------------------------------------------------
    // Lifecycle callbacks
    // -------------------------------------------------------------------------

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public Integer getRestingHR() {
        return restingHR;
    }

    public void setRestingHR(Integer restingHR) {
        this.restingHR = restingHR;
    }

    public Integer getMaxHR() {
        return maxHR;
    }

    public void setMaxHR(Integer maxHR) {
        this.maxHR = maxHR;
    }

    public Integer getLthr() {
        return lthr;
    }

    public void setLthr(Integer lthr) {
        this.lthr = lthr;
    }

    public Integer getThresholdPaceSecondsPerKm() {
        return thresholdPaceSecondsPerKm;
    }

    public void setThresholdPaceSecondsPerKm(Integer thresholdPaceSecondsPerKm) {
        this.thresholdPaceSecondsPerKm = thresholdPaceSecondsPerKm;
    }

    public BigDecimal getVo2Max() {
        return vo2Max;
    }

    public void setVo2Max(BigDecimal vo2Max) {
        this.vo2Max = vo2Max;
    }

    public Integer getFiveKSeconds() {
        return fiveKSeconds;
    }

    public void setFiveKSeconds(Integer fiveKSeconds) {
        this.fiveKSeconds = fiveKSeconds;
    }

    public Integer getTenKSeconds() {
        return tenKSeconds;
    }

    public void setTenKSeconds(Integer tenKSeconds) {
        this.tenKSeconds = tenKSeconds;
    }

    public Integer getHalfMarathonSeconds() {
        return halfMarathonSeconds;
    }

    public void setHalfMarathonSeconds(Integer halfMarathonSeconds) {
        this.halfMarathonSeconds = halfMarathonSeconds;
    }

    public Integer getMarathonSeconds() {
        return marathonSeconds;
    }

    public void setMarathonSeconds(Integer marathonSeconds) {
        this.marathonSeconds = marathonSeconds;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
