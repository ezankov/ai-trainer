package com.trainer.profile;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link HrProfile} entities.
 *
 * <p>Provides standard CRUD operations via {@link JpaRepository} plus
 * lookup and deletion methods keyed on the parent athlete profile.
 */
public interface HrProfileRepository extends JpaRepository<HrProfile, Long> {

    /**
     * Finds an HR profile by the parent athlete profile's ID.
     *
     * @param athleteProfileId the athlete profile ID to search for
     * @return an {@link Optional} containing the HR profile if found, or empty otherwise
     */
    Optional<HrProfile> findByAthleteProfileId(Long athleteProfileId);

    /**
     * Deletes the HR profile associated with the given athlete profile ID.
     *
     * @param athleteProfileId the athlete profile ID whose HR profile should be deleted
     */
    @Transactional
    void deleteByAthleteProfileId(Long athleteProfileId);
}
