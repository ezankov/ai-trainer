package com.trainer.workout;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Workout} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} plus
 * query-derivation methods for user-scoped workout retrieval.
 */
public interface WorkoutRepository extends JpaRepository<Workout, UUID> {

    /**
     * Returns all workouts belonging to a user, ordered by creation date descending.
     *
     * @param userId the owning user's ID
     * @return workouts ordered most-recent-first
     */
    List<Workout> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Returns all workouts belonging to a user filtered by sport type,
     * ordered by creation date descending.
     *
     * @param userId    the owning user's ID
     * @param sportType the sport type to filter by
     * @return matching workouts ordered most-recent-first
     */
    List<Workout> findByUserIdAndSportTypeOrderByCreatedAtDesc(Long userId, SportType sportType);

    /**
     * Finds a workout by its ID and owning user's ID.
     * Used for ownership-checked retrieval (returns empty if the workout
     * does not exist or belongs to a different user).
     *
     * @param id     the workout UUID
     * @param userId the owning user's ID
     * @return an {@link Optional} containing the workout if found and owned, or empty otherwise
     */
    Optional<Workout> findByIdAndUserId(UUID id, Long userId);
}
