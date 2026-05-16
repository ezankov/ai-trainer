package com.trainer.workout;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service layer for workout CRUD operations.
 * Handles enum parsing, step validation, persistence, and DTO mapping.
 */
@Service
@Transactional
public class WorkoutService {

    private final WorkoutRepository workoutRepository;
    private final WorkoutStepValidator workoutStepValidator;

    public WorkoutService(WorkoutRepository workoutRepository, WorkoutStepValidator workoutStepValidator) {
        this.workoutRepository = workoutRepository;
        this.workoutStepValidator = workoutStepValidator;
    }

    /**
     * Creates a new workout with validated steps.
     */
    public WorkoutResponse createWorkout(Long userId, CreateWorkoutRequest request) {
        SportType sportType = parseSportType(request.sportType());
        SubSport subSport = parseSubSport(request.subSport());

        workoutStepValidator.validate(request.steps());

        Workout workout = new Workout();
        workout.setUserId(userId);
        workout.setName(request.name());
        workout.setSportType(sportType);
        workout.setSubSport(subSport);
        workout.setNumValidSteps(request.steps().size());

        List<WorkoutStep> steps = buildSteps(request.steps(), workout);
        workout.setSteps(steps);

        Workout saved = workoutRepository.save(workout);
        return toWorkoutResponse(saved);
    }

    /**
     * Returns all workouts for a user, optionally filtered by sport type,
     * ordered by createdAt descending.
     */
    @Transactional(readOnly = true)
    public List<WorkoutSummaryResponse> getWorkouts(Long userId, SportType sportType) {
        List<Workout> workouts;
        if (sportType != null) {
            workouts = workoutRepository.findByUserIdAndSportTypeOrderByCreatedAtDesc(userId, sportType);
        } else {
            workouts = workoutRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return workouts.stream()
                .map(this::toWorkoutSummaryResponse)
                .toList();
    }

    /**
     * Returns a full workout with steps. Throws WorkoutNotFoundException if not found or not owned.
     */
    @Transactional(readOnly = true)
    public WorkoutResponse getWorkout(Long userId, UUID workoutId) {
        Workout workout = workoutRepository.findByIdAndUserId(workoutId, userId)
                .orElseThrow(() -> new WorkoutNotFoundException("Workout not found"));
        return toWorkoutResponse(workout);
    }

    /**
     * Updates an existing workout with full replacement semantics.
     * Clears existing steps and replaces with new validated steps.
     */
    public WorkoutResponse updateWorkout(Long userId, UUID workoutId, UpdateWorkoutRequest request) {
        Workout workout = workoutRepository.findByIdAndUserId(workoutId, userId)
                .orElseThrow(() -> new WorkoutNotFoundException("Workout not found"));

        SportType sportType = parseSportType(request.sportType());
        SubSport subSport = parseSubSport(request.subSport());

        workoutStepValidator.validate(request.steps());

        workout.setName(request.name());
        workout.setSportType(sportType);
        workout.setSubSport(subSport);
        workout.setNumValidSteps(request.steps().size());

        // Clear existing steps (orphanRemoval will delete them)
        workout.getSteps().clear();

        // Add new steps
        List<WorkoutStep> newSteps = buildSteps(request.steps(), workout);
        workout.getSteps().addAll(newSteps);

        Workout saved = workoutRepository.save(workout);
        return toWorkoutResponse(saved);
    }

    /**
     * Deletes a workout owned by the user. Cascade removes all steps.
     * Throws WorkoutNotFoundException if not found or not owned.
     */
    public void deleteWorkout(Long userId, UUID workoutId) {
        Workout workout = workoutRepository.findByIdAndUserId(workoutId, userId)
                .orElseThrow(() -> new WorkoutNotFoundException("Workout not found"));
        workoutRepository.delete(workout);
    }

    // -------------------------------------------------------------------------
    // Enum parsing helpers
    // -------------------------------------------------------------------------

    private SportType parseSportType(String value) {
        try {
            return SportType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new WorkoutValidationException(List.of(
                    new WorkoutValidationException.StepValidationError(-1, "sportType",
                            "Invalid sport type: " + value)));
        }
    }

    private SubSport parseSubSport(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return SubSport.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new WorkoutValidationException(List.of(
                    new WorkoutValidationException.StepValidationError(-1, "subSport",
                            "Invalid sub sport: " + value)));
        }
    }

    // -------------------------------------------------------------------------
    // Step building
    // -------------------------------------------------------------------------

    private List<WorkoutStep> buildSteps(List<WorkoutStepRequest> stepRequests, Workout workout) {
        List<WorkoutStep> steps = new ArrayList<>();
        for (int i = 0; i < stepRequests.size(); i++) {
            WorkoutStepRequest req = stepRequests.get(i);
            WorkoutStep step = new WorkoutStep();
            step.setWorkout(workout);
            step.setStepOrder(i);
            step.setStepName(req.stepName());
            step.setIntensity(Intensity.valueOf(req.intensity()));
            step.setDurationType(DurationType.valueOf(req.durationType()));
            step.setDurationValue(req.durationValue());
            step.setTargetType(TargetType.valueOf(req.targetType()));
            step.setTargetValueLow(req.targetValueLow());
            step.setTargetValueHigh(req.targetValueHigh());
            step.setNotes(req.notes());
            steps.add(step);
        }
        return steps;
    }

    // -------------------------------------------------------------------------
    // DTO mapping helpers
    // -------------------------------------------------------------------------

    private WorkoutResponse toWorkoutResponse(Workout workout) {
        List<WorkoutStepResponse> stepResponses = workout.getSteps().stream()
                .map(this::toWorkoutStepResponse)
                .toList();

        return new WorkoutResponse(
                workout.getId(),
                workout.getName(),
                workout.getSportType().name(),
                workout.getSubSport() != null ? workout.getSubSport().name() : null,
                workout.getNumValidSteps(),
                workout.getCreatedAt(),
                workout.getUpdatedAt(),
                stepResponses
        );
    }

    private WorkoutSummaryResponse toWorkoutSummaryResponse(Workout workout) {
        return new WorkoutSummaryResponse(
                workout.getId(),
                workout.getName(),
                workout.getSportType().name(),
                workout.getSubSport() != null ? workout.getSubSport().name() : null,
                workout.getNumValidSteps(),
                workout.getCreatedAt(),
                workout.getUpdatedAt()
        );
    }

    private WorkoutStepResponse toWorkoutStepResponse(WorkoutStep step) {
        return new WorkoutStepResponse(
                step.getId(),
                step.getStepOrder(),
                step.getStepName(),
                step.getIntensity().name(),
                step.getDurationType().name(),
                step.getDurationValue(),
                step.getTargetType().name(),
                step.getTargetValueLow(),
                step.getTargetValueHigh(),
                step.getNotes()
        );
    }
}
