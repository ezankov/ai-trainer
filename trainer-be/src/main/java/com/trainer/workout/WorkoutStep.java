package com.trainer.workout;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * JPA entity mapped to the {@code trainer.workout_steps} table.
 * Represents a single step within a workout, defining duration, target, and intensity.
 */
@Entity
@Table(name = "workout_steps", schema = "trainer",
        uniqueConstraints = @UniqueConstraint(columnNames = {"workout_id", "step_order"}))
public class WorkoutStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_name", length = 50)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Intensity intensity;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_type", nullable = false, length = 40)
    private DurationType durationType;

    @Column(name = "duration_value")
    private Integer durationValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private TargetType targetType;

    @Column(name = "target_value_low")
    private Integer targetValueLow;

    @Column(name = "target_value_high")
    private Integer targetValueHigh;

    @Column(length = 255)
    private String notes;

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Workout getWorkout() {
        return workout;
    }

    public void setWorkout(Workout workout) {
        this.workout = workout;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public Intensity getIntensity() {
        return intensity;
    }

    public void setIntensity(Intensity intensity) {
        this.intensity = intensity;
    }

    public DurationType getDurationType() {
        return durationType;
    }

    public void setDurationType(DurationType durationType) {
        this.durationType = durationType;
    }

    public Integer getDurationValue() {
        return durationValue;
    }

    public void setDurationValue(Integer durationValue) {
        this.durationValue = durationValue;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }

    public Integer getTargetValueLow() {
        return targetValueLow;
    }

    public void setTargetValueLow(Integer targetValueLow) {
        this.targetValueLow = targetValueLow;
    }

    public Integer getTargetValueHigh() {
        return targetValueHigh;
    }

    public void setTargetValueHigh(Integer targetValueHigh) {
        this.targetValueHigh = targetValueHigh;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
