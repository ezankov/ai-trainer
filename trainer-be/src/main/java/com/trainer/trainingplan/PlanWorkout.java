package com.trainer.trainingplan;

import com.trainer.workout.Workout;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * JPA entity mapped to the {@code trainer.plan_workouts} table.
 * Links a training plan to a workout with scheduling metadata (week, day, order).
 */
@Entity
@Table(name = "plan_workouts", schema = "trainer",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"training_plan_id", "week_number", "day_of_week", "order_in_day"}))
public class PlanWorkout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "training_plan_id", nullable = false)
    private TrainingPlan trainingPlan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "order_in_day", nullable = false)
    private Integer orderInDay;

    // -------------------------------------------------------------------------
    // Getters and setters
    // -------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TrainingPlan getTrainingPlan() {
        return trainingPlan;
    }

    public void setTrainingPlan(TrainingPlan trainingPlan) {
        this.trainingPlan = trainingPlan;
    }

    public Workout getWorkout() {
        return workout;
    }

    public void setWorkout(Workout workout) {
        this.workout = workout;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Integer getOrderInDay() {
        return orderInDay;
    }

    public void setOrderInDay(Integer orderInDay) {
        this.orderInDay = orderInDay;
    }
}
