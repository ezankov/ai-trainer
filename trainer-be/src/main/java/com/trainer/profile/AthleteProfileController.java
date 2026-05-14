package com.trainer.profile;

import com.trainer.auth.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for athlete profile CRUD operations.
 * All endpoints require authentication — the user ID is extracted from the SecurityContext.
 */
@RestController
@RequestMapping("/api/athlete-profile")
public class AthleteProfileController {

    private final AthleteProfileService athleteProfileService;

    public AthleteProfileController(AthleteProfileService athleteProfileService) {
        this.athleteProfileService = athleteProfileService;
    }

    @PostMapping
    public ResponseEntity<ProfileResponse> createProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateProfileRequest request) {
        ProfileResponse response = athleteProfileService.createProfile(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ProfileResponse getProfile(@AuthenticationPrincipal User user) {
        return athleteProfileService.getProfile(user.getId());
    }

    @PutMapping
    public ProfileResponse updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateProfileRequest request) {
        return athleteProfileService.updateProfile(user.getId(), request);
    }
}
