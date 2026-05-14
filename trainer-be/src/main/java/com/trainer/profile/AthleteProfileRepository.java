package com.trainer.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link AthleteProfile} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} plus
 * lookup methods for the one-profile-per-user constraint.
 */
public interface AthleteProfileRepository extends JpaRepository<AthleteProfile, Long> {

    /**
     * Finds an athlete profile by the owning user's ID.
     *
     * @param userId the user ID to search for
     * @return an {@link Optional} containing the profile if found, or empty otherwise
     */
    Optional<AthleteProfile> findByUserId(Long userId);

    /**
     * Checks whether an athlete profile already exists for the given user.
     *
     * @param userId the user ID to check
     * @return {@code true} if a profile exists for that user, {@code false} otherwise
     */
    boolean existsByUserId(Long userId);
}
