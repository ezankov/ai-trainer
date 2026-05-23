package com.trainer.ai;

import com.trainer.trainingplan.TrainingPlan;
import com.trainer.workout.DurationType;
import com.trainer.workout.Intensity;
import com.trainer.workout.SportType;
import com.trainer.workout.SubSport;
import com.trainer.workout.TargetType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the parsed AI response against plan constraints.
 * Collects ALL validation errors and throws a single
 * {@link AiResponseValidationException} with the full list.
 */
@Component
public class AiResponseValidator {

    /**
     * Validates the parsed AI response against plan constraints.
     *
     * @param response the parsed AI plan response
     * @param plan     the training plan entity providing duration constraints
     * @throws AiResponseValidationException with details about which fields failed
     */
    public void validate(AiPlanResponse response, TrainingPlan plan) {
        List<String> errors = new ArrayList<>();

        if (response.workouts() == null || response.workouts().isEmpty()) {
            errors.add("workouts array must not be empty");
            throw new AiResponseValidationException(
                    "AI response validation failed: " + errors.size() + " error(s)", errors);
        }

        int maxWeeks = plan.getDuration().getWeeks();

        for (int i = 0; i < response.workouts().size(); i++) {
            AiWorkoutResponse workout = response.workouts().get(i);
            validateWorkout(workout, i, maxWeeks, errors);
        }

        if (!errors.isEmpty()) {
            throw new AiResponseValidationException(
                    "AI response validation failed: " + errors.size() + " error(s)", errors);
        }
    }

    private void validateWorkout(AiWorkoutResponse workout, int index, int maxWeeks, List<String> errors) {
        String prefix = "workouts[" + index + "]";

        // Validate name
        if (workout.name() == null || workout.name().isBlank()) {
            errors.add(prefix + ".name must not be blank");
        } else if (workout.name().length() > 50) {
            errors.add(prefix + ".name must be at most 50 characters");
        }

        // Validate sportType
        if (workout.sportType() == null || !isValidEnum(SportType.class, workout.sportType())) {
            errors.add(prefix + ".sportType must be a valid SportType enum value");
        }

        // Validate subSport (null is allowed)
        if (workout.subSport() != null && !isValidEnum(SubSport.class, workout.subSport())) {
            errors.add(prefix + ".subSport must be null or a valid SubSport enum value");
        }

        // Validate steps
        if (workout.steps() == null || workout.steps().isEmpty()) {
            errors.add(prefix + ".steps must contain at least 1 step");
        } else {
            validateSteps(workout.steps(), prefix, errors);
        }

        // Validate schedule
        if (workout.schedule() != null) {
            validateSchedule(workout.schedule(), prefix, maxWeeks, errors);
        }
    }

    private void validateSteps(List<AiWorkoutStepResponse> steps, String prefix, List<String> errors) {
        for (int i = 0; i < steps.size(); i++) {
            AiWorkoutStepResponse step = steps.get(i);
            String stepPrefix = prefix + ".steps[" + i + "]";

            // Validate stepOrder is sequential starting at 1
            int expectedOrder = i + 1;
            if (step.stepOrder() == null || step.stepOrder() != expectedOrder) {
                errors.add(stepPrefix + ".stepOrder must be " + expectedOrder + " (sequential starting at 1)");
            }

            // Validate intensity
            if (step.intensity() == null || !isValidEnum(Intensity.class, step.intensity())) {
                errors.add(stepPrefix + ".intensity must be a valid Intensity enum value");
            }

            // Validate durationType
            if (step.durationType() == null || !isValidEnum(DurationType.class, step.durationType())) {
                errors.add(stepPrefix + ".durationType must be a valid DurationType enum value");
            } else {
                // Validate durationValue based on durationType
                validateDurationValue(step, stepPrefix, errors);
            }

            // Validate targetType
            if (step.targetType() == null || !isValidEnum(TargetType.class, step.targetType())) {
                errors.add(stepPrefix + ".targetType must be a valid TargetType enum value");
            }
        }
    }

    private void validateDurationValue(AiWorkoutStepResponse step, String stepPrefix, List<String> errors) {
        DurationType durationType;
        try {
            durationType = DurationType.valueOf(step.durationType());
        } catch (IllegalArgumentException e) {
            // Already reported as invalid enum; skip range check
            return;
        }

        Integer value = step.durationValue();

        switch (durationType) {
            case TIME -> {
                if (value == null || value < 1 || value > 86400) {
                    errors.add(stepPrefix + ".durationValue must be between 1 and 86400 for TIME (seconds)");
                }
            }
            case DISTANCE -> {
                if (value == null || value < 1 || value > 100000) {
                    errors.add(stepPrefix + ".durationValue must be between 1 and 100000 for DISTANCE (metres)");
                }
            }
            case REPEAT_UNTIL_STEPS_COMPLETE -> {
                if (value == null || value < 1) {
                    errors.add(stepPrefix + ".durationValue must be at least 1 for REPEAT_UNTIL_STEPS_COMPLETE");
                }
            }
            default -> {
                // Other types: any positive integer
                if (value == null || value < 1) {
                    errors.add(stepPrefix + ".durationValue must be a positive integer");
                }
            }
        }
    }

    private void validateSchedule(AiScheduleResponse schedule, String prefix, int maxWeeks, List<String> errors) {
        if (schedule.weekNumber() == null || schedule.weekNumber() < 1 || schedule.weekNumber() > maxWeeks) {
            errors.add(prefix + ".schedule.weekNumber must be between 1 and " + maxWeeks);
        }

        if (schedule.dayOfWeek() == null || schedule.dayOfWeek() < 1 || schedule.dayOfWeek() > 7) {
            errors.add(prefix + ".schedule.dayOfWeek must be between 1 and 7");
        }

        if (schedule.orderInDay() == null || schedule.orderInDay() < 1 || schedule.orderInDay() > 10) {
            errors.add(prefix + ".schedule.orderInDay must be between 1 and 10");
        }
    }

    private <E extends Enum<E>> boolean isValidEnum(Class<E> enumClass, String value) {
        try {
            Enum.valueOf(enumClass, value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
