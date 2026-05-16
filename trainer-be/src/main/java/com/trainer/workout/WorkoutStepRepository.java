package com.trainer.workout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link WorkoutStep} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} plus
 * a bulk-delete method used during workout updates (full step replacement).
 */
public interface WorkoutStepRepository extends JpaRepository<WorkoutStep, UUID> {

    /**
     * Deletes all workout steps belonging to the specified workout.
     * Used during full-replacement updates to clear existing steps
     * before persisting the new set.
     *
     * @param workoutId the parent workout's UUID
     */
    void deleteByWorkoutId(UUID workoutId);
}
