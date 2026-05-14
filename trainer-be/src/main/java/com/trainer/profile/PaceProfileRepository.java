package com.trainer.profile;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link PaceProfile} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} plus
 * lookup and deletion methods keyed on the parent athlete profile.
 */
public interface PaceProfileRepository extends JpaRepository<PaceProfile, Long> {

    /**
     * Finds a pace profile by the parent athlete profile's ID.
     *
     * @param athleteProfileId the athlete profile ID to search for
     * @return an {@link Optional} containing the pace profile if found, or empty otherwise
     */
    Optional<PaceProfile> findByAthleteProfileId(Long athleteProfileId);

    /**
     * Deletes the pace profile associated with the given athlete profile ID.
     *
     * @param athleteProfileId the athlete profile ID whose pace profile should be deleted
     */
    @Transactional
    void deleteByAthleteProfileId(Long athleteProfileId);
}
