package com.trainer.trainingplan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TrainingPlan} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} plus
 * query methods for user-scoped plan retrieval and state filtering.
 */
public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, UUID> {

    /**
     * Finds all training plans belonging to a user, ordered by creation date descending.
     *
     * @param userId the ID of the owning user
     * @return list of training plans ordered by most recently created first
     */
    List<TrainingPlan> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Finds a training plan by its ID and owning user ID.
     * Used for ownership-checked retrieval (returns empty rather than 403 to avoid leaking existence).
     *
     * @param id     the plan UUID
     * @param userId the ID of the owning user
     * @return an {@link Optional} containing the plan if found and owned, or empty otherwise
     */
    Optional<TrainingPlan> findByIdAndUserId(UUID id, Long userId);

    /**
     * Finds a training plan for a user in a specific state.
     * Primarily used to locate the current active plan.
     *
     * @param userId the ID of the owning user
     * @param state  the plan state to filter by
     * @return an {@link Optional} containing the plan if found, or empty otherwise
     */
    Optional<TrainingPlan> findByUserIdAndState(Long userId, PlanState state);
}
