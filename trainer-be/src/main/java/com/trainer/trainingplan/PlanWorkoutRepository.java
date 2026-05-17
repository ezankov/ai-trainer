package com.trainer.trainingplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PlanWorkout} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} plus
 * ordered retrieval of workout assignments for a given training plan.
 */
public interface PlanWorkoutRepository extends JpaRepository<PlanWorkout, UUID> {

    /**
     * Finds all plan-workout assignments for a training plan, ordered by
     * week number ascending, day of week ascending, and order within day ascending.
     *
     * @param trainingPlanId the UUID of the training plan
     * @return list of plan workouts in chronological schedule order
     */
    List<PlanWorkout> findByTrainingPlanIdOrderByWeekNumberAscDayOfWeekAscOrderInDayAsc(UUID trainingPlanId);
}
