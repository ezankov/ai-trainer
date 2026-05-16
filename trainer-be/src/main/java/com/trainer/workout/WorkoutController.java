package com.trainer.workout;

import com.trainer.auth.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for workout CRUD operations.
 * All endpoints require authentication — the user ID is extracted from the SecurityContext.
 */
@RestController
@RequestMapping("/api/workouts")
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkoutResponse createWorkout(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateWorkoutRequest request) {
        return workoutService.createWorkout(user.getId(), request);
    }

    @GetMapping
    public List<WorkoutSummaryResponse> getWorkouts(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String sportType) {
        SportType parsedSportType = parseSportTypeParam(sportType);
        return workoutService.getWorkouts(user.getId(), parsedSportType);
    }

    @GetMapping("/{id}")
    public WorkoutResponse getWorkout(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        return workoutService.getWorkout(user.getId(), id);
    }

    @PutMapping("/{id}")
    public WorkoutResponse updateWorkout(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkoutRequest request) {
        return workoutService.updateWorkout(user.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWorkout(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        workoutService.deleteWorkout(user.getId(), id);
    }

    /**
     * Parses the sportType query parameter string to a SportType enum value.
     * Returns null if the parameter is null or blank.
     * Throws WorkoutValidationException if the value does not match any enum constant.
     */
    private SportType parseSportTypeParam(String sportType) {
        if (sportType == null || sportType.isBlank()) {
            return null;
        }
        try {
            return SportType.valueOf(sportType);
        } catch (IllegalArgumentException e) {
            throw new WorkoutValidationException(List.of(
                    new WorkoutValidationException.StepValidationError(-1, "sportType",
                            "Invalid sport type: " + sportType)));
        }
    }
}
