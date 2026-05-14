package com.trainer.profile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service layer for athlete profile CRUD operations.
 * Handles validation, persistence, and zone calculation orchestration.
 */
@Service
public class AthleteProfileService {

    private final AthleteProfileRepository athleteProfileRepository;
    private final HrProfileRepository hrProfileRepository;
    private final PaceProfileRepository paceProfileRepository;
    private final HrZoneCalculator hrZoneCalculator;
    private final PaceZoneCalculator paceZoneCalculator;
    private final ProfileRequestValidator validator;

    public AthleteProfileService(
            AthleteProfileRepository athleteProfileRepository,
            HrProfileRepository hrProfileRepository,
            PaceProfileRepository paceProfileRepository,
            HrZoneCalculator hrZoneCalculator,
            PaceZoneCalculator paceZoneCalculator,
            ProfileRequestValidator validator
    ) {
        this.athleteProfileRepository = athleteProfileRepository;
        this.hrProfileRepository = hrProfileRepository;
        this.paceProfileRepository = paceProfileRepository;
        this.hrZoneCalculator = hrZoneCalculator;
        this.paceZoneCalculator = paceZoneCalculator;
        this.validator = validator;
    }

    /**
     * Creates a new athlete profile for the given user.
     *
     * @param userId  the authenticated user's ID
     * @param request the profile creation request
     * @return the created profile response including any calculated zones
     * @throws ProfileAlreadyExistsException if the user already has a profile
     * @throws ProfileValidationException    if cross-field validation fails
     */
    @Transactional
    public ProfileResponse createProfile(Long userId, CreateProfileRequest request) {
        validator.validate(request);

        if (athleteProfileRepository.existsByUserId(userId)) {
            throw new ProfileAlreadyExistsException();
        }

        AthleteProfile profile = mapToEntity(userId, request);
        profile = athleteProfileRepository.save(profile);

        HrProfile hrProfile = null;
        if (request.lthr() != null) {
            hrProfile = createHrProfile(profile.getId(), request.restingHR(), request.lthr(), request.maxHR());
        }

        PaceProfile paceProfile = null;
        if (request.thresholdPaceSecondsPerKm() != null) {
            paceProfile = createPaceProfile(profile.getId(), request.thresholdPaceSecondsPerKm());
        }

        return mapToResponse(profile, hrProfile, paceProfile);
    }

    /**
     * Retrieves the athlete profile for the given user.
     *
     * @param userId the authenticated user's ID
     * @return the profile response including any associated zones
     * @throws ProfileNotFoundException if no profile exists for the user
     */
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        AthleteProfile profile = athleteProfileRepository.findByUserId(userId)
                .orElseThrow(ProfileNotFoundException::new);

        HrProfile hrProfile = hrProfileRepository.findByAthleteProfileId(profile.getId()).orElse(null);
        PaceProfile paceProfile = paceProfileRepository.findByAthleteProfileId(profile.getId()).orElse(null);

        return mapToResponse(profile, hrProfile, paceProfile);
    }

    /**
     * Updates the athlete profile for the given user (full replacement).
     *
     * @param userId  the authenticated user's ID
     * @param request the profile update request
     * @return the updated profile response including recalculated zones
     * @throws ProfileNotFoundException   if no profile exists for the user
     * @throws ProfileValidationException if cross-field validation fails
     */
    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        validator.validate(request);

        AthleteProfile profile = athleteProfileRepository.findByUserId(userId)
                .orElseThrow(ProfileNotFoundException::new);

        updateEntityFields(profile, request);
        profile = athleteProfileRepository.save(profile);

        HrProfile hrProfile = handleHrProfileUpdate(profile.getId(), request);
        PaceProfile paceProfile = handlePaceProfileUpdate(profile.getId(), request);

        return mapToResponse(profile, hrProfile, paceProfile);
    }

    // -------------------------------------------------------------------------
    // HR Profile handling
    // -------------------------------------------------------------------------

    private HrProfile createHrProfile(Long athleteProfileId, int restingHR, int lthr, int maxHR) {
        List<HrZone> zones = hrZoneCalculator.calculate(restingHR, lthr, maxHR);

        HrProfile hrProfile = new HrProfile();
        hrProfile.setAthleteProfileId(athleteProfileId);
        hrProfile = hrProfileRepository.save(hrProfile);

        for (HrZone zone : zones) {
            zone.setHrProfileId(hrProfile.getId());
        }
        hrProfile.setZones(new ArrayList<>(zones));
        return hrProfileRepository.save(hrProfile);
    }

    private HrProfile handleHrProfileUpdate(Long athleteProfileId, UpdateProfileRequest request) {
        if (request.lthr() != null) {
            // Delete existing HR profile and recalculate
            hrProfileRepository.deleteByAthleteProfileId(athleteProfileId);
            return createHrProfile(athleteProfileId, request.restingHR(), request.lthr(), request.maxHR());
        } else {
            // Remove HR profile if lthr is null
            hrProfileRepository.deleteByAthleteProfileId(athleteProfileId);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Pace Profile handling
    // -------------------------------------------------------------------------

    private PaceProfile createPaceProfile(Long athleteProfileId, int thresholdPaceSecondsPerKm) {
        List<PaceZone> zones = paceZoneCalculator.calculate(thresholdPaceSecondsPerKm);

        PaceProfile paceProfile = new PaceProfile();
        paceProfile.setAthleteProfileId(athleteProfileId);
        paceProfile = paceProfileRepository.save(paceProfile);

        for (PaceZone zone : zones) {
            zone.setPaceProfileId(paceProfile.getId());
        }
        paceProfile.setZones(new ArrayList<>(zones));
        return paceProfileRepository.save(paceProfile);
    }

    private PaceProfile handlePaceProfileUpdate(Long athleteProfileId, UpdateProfileRequest request) {
        if (request.thresholdPaceSecondsPerKm() != null) {
            // Delete existing Pace profile and recalculate
            paceProfileRepository.deleteByAthleteProfileId(athleteProfileId);
            return createPaceProfile(athleteProfileId, request.thresholdPaceSecondsPerKm());
        } else {
            // Remove Pace profile if threshold pace is null
            paceProfileRepository.deleteByAthleteProfileId(athleteProfileId);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Mapping helpers
    // -------------------------------------------------------------------------

    private AthleteProfile mapToEntity(Long userId, CreateProfileRequest request) {
        AthleteProfile profile = new AthleteProfile();
        profile.setUserId(userId);
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setWeightKg(request.weightKg());
        profile.setRestingHR(request.restingHR());
        profile.setMaxHR(request.maxHR());
        profile.setLthr(request.lthr());
        profile.setThresholdPaceSecondsPerKm(request.thresholdPaceSecondsPerKm());
        profile.setVo2Max(request.vo2Max());
        profile.setFiveKSeconds(request.fiveKSeconds());
        profile.setTenKSeconds(request.tenKSeconds());
        profile.setHalfMarathonSeconds(request.halfMarathonSeconds());
        profile.setMarathonSeconds(request.marathonSeconds());
        return profile;
    }

    private void updateEntityFields(AthleteProfile profile, UpdateProfileRequest request) {
        profile.setDateOfBirth(request.dateOfBirth());
        profile.setWeightKg(request.weightKg());
        profile.setRestingHR(request.restingHR());
        profile.setMaxHR(request.maxHR());
        profile.setLthr(request.lthr());
        profile.setThresholdPaceSecondsPerKm(request.thresholdPaceSecondsPerKm());
        profile.setVo2Max(request.vo2Max());
        profile.setFiveKSeconds(request.fiveKSeconds());
        profile.setTenKSeconds(request.tenKSeconds());
        profile.setHalfMarathonSeconds(request.halfMarathonSeconds());
        profile.setMarathonSeconds(request.marathonSeconds());
    }

    private ProfileResponse mapToResponse(AthleteProfile profile, HrProfile hrProfile, PaceProfile paceProfile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getDateOfBirth(),
                profile.getWeightKg(),
                profile.getRestingHR(),
                profile.getMaxHR(),
                profile.getLthr(),
                profile.getThresholdPaceSecondsPerKm(),
                profile.getVo2Max(),
                profile.getFiveKSeconds(),
                profile.getTenKSeconds(),
                profile.getHalfMarathonSeconds(),
                profile.getMarathonSeconds(),
                mapHrProfileResponse(hrProfile),
                mapPaceProfileResponse(paceProfile)
        );
    }

    private HrProfileResponse mapHrProfileResponse(HrProfile hrProfile) {
        if (hrProfile == null) {
            return null;
        }
        List<HrZoneResponse> zones = hrProfile.getZones().stream()
                .map(zone -> new HrZoneResponse(
                        zone.getZoneNumber(),
                        zone.getName(),
                        zone.getLowerBound(),
                        zone.getUpperBound()
                ))
                .toList();
        return new HrProfileResponse(zones);
    }

    private PaceProfileResponse mapPaceProfileResponse(PaceProfile paceProfile) {
        if (paceProfile == null) {
            return null;
        }
        List<PaceZoneResponse> zones = paceProfile.getZones().stream()
                .map(zone -> new PaceZoneResponse(
                        zone.getZoneNumber(),
                        zone.getName(),
                        zone.getLowerBound(),
                        zone.getUpperBound()
                ))
                .toList();
        return new PaceProfileResponse(zones);
    }
}
