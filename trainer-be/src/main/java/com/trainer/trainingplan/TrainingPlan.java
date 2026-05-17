package com.trainer.trainingplan;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity mapped to the {@code trainer.training_plans} table.
 * Represents a structured multi-week training plan for a user.
 */
@Entity
@Table(name = "training_plans", schema = "trainer")
public class TrainingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanDistance distance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PlanDuration duration;

    @Column(name = "race_date", nullable = false)
    private LocalDate raceDate;

    @Column(name = "target_pace_seconds_per_km", nullable = false)
    private Integer targetPaceSecondsPerKm;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_model", nullable = false, length = 20)
    private AiModel aiModel;

    @Column(name = "training_days", nullable = false, columnDefinition = "integer[]")
    private List<Integer> trainingDays;

    @Column(name = "long_run_day", nullable = false)
    private Integer longRunDay;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PlanState state;

    @OneToMany(mappedBy = "trainingPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("weekNumber ASC, dayOfWeek ASC, orderInDay ASC")
    private List<PlanWorkout> planWorkouts = new ArrayList<>();

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
        if (this.state == null) {
            this.state = PlanState.NEW;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public PlanDistance getDistance() {
        return distance;
    }

    public void setDistance(PlanDistance distance) {
        this.distance = distance;
    }

    public PlanDuration getDuration() {
        return duration;
    }

    public void setDuration(PlanDuration duration) {
        this.duration = duration;
    }

    public LocalDate getRaceDate() {
        return raceDate;
    }

    public void setRaceDate(LocalDate raceDate) {
        this.raceDate = raceDate;
    }

    public Integer getTargetPaceSecondsPerKm() {
        return targetPaceSecondsPerKm;
    }

    public void setTargetPaceSecondsPerKm(Integer targetPaceSecondsPerKm) {
        this.targetPaceSecondsPerKm = targetPaceSecondsPerKm;
    }

    public AiModel getAiModel() {
        return aiModel;
    }

    public void setAiModel(AiModel aiModel) {
        this.aiModel = aiModel;
    }

    public List<Integer> getTrainingDays() {
        return trainingDays;
    }

    public void setTrainingDays(List<Integer> trainingDays) {
        this.trainingDays = trainingDays;
    }

    public Integer getLongRunDay() {
        return longRunDay;
    }

    public void setLongRunDay(Integer longRunDay) {
        this.longRunDay = longRunDay;
    }

    public PlanState getState() {
        return state;
    }

    public void setState(PlanState state) {
        this.state = state;
    }

    public List<PlanWorkout> getPlanWorkouts() {
        return planWorkouts;
    }

    public void setPlanWorkouts(List<PlanWorkout> planWorkouts) {
        this.planWorkouts = planWorkouts;
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
