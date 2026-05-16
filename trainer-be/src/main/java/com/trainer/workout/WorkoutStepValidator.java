package com.trainer.workout;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates a list of {@link WorkoutStepRequest} objects against Garmin FIT SDK constraints.
 * <p>
 * This is a pure validation function — it does not interact with the database.
 * It validates the step list structure and values including:
 * <ul>
 *   <li>Duration value range checks based on durationType</li>
 *   <li>Target value range checks based on targetType</li>
 *   <li>targetValueLow &lt;= targetValueHigh for ranged targets</li>
 *   <li>Repeat step: durationValue references a valid preceding step index</li>
 *   <li>Repeat step: targetValueLow (repetitions) in [1, 100]</li>
 *   <li>Repeat step: intensity must be REST, targetType must be OPEN</li>
 *   <li>No nested repeats</li>
 * </ul>
 * All errors are collected and thrown together in a single {@link WorkoutValidationException}.
 */
@Component
public class WorkoutStepValidator {

    /**
     * Validates the given list of workout steps.
     *
     * @param steps the list of step requests to validate
     * @throws WorkoutValidationException if any step fails validation
     */
    public void validate(List<WorkoutStepRequest> steps) {
        List<WorkoutValidationException.StepValidationError> errors = new ArrayList<>();

        for (int i = 0; i < steps.size(); i++) {
            WorkoutStepRequest step = steps.get(i);
            DurationType durationType = parseDurationType(step, i, errors);
            TargetType targetType = parseTargetType(step, i, errors);
            Intensity intensity = parseIntensity(step, i, errors);

            if (durationType != null) {
                if (durationType == DurationType.REPEAT_UNTIL_STEPS_COMPLETE) {
                    validateRepeatStep(step, i, steps, intensity, targetType, errors);
                } else {
                    validateDurationValue(step, i, durationType, errors);
                }
            }

            if (targetType != null && durationType != DurationType.REPEAT_UNTIL_STEPS_COMPLETE) {
                validateTargetValues(step, i, targetType, errors);
            }
        }

        if (!errors.isEmpty()) {
            throw new WorkoutValidationException(errors);
        }
    }

    private DurationType parseDurationType(WorkoutStepRequest step, int index,
                                           List<WorkoutValidationException.StepValidationError> errors) {
        if (step.durationType() == null) {
            errors.add(new WorkoutValidationException.StepValidationError(
                    index, "durationType", "Duration type is required"));
            return null;
        }
        try {
            return DurationType.valueOf(step.durationType());
        } catch (IllegalArgumentException e) {
            errors.add(new WorkoutValidationException.StepValidationError(
                    index, "durationType", "Invalid duration type: " + step.durationType()));
            return null;
        }
    }

    private TargetType parseTargetType(WorkoutStepRequest step, int index,
                                       List<WorkoutValidationException.StepValidationError> errors) {
        if (step.targetType() == null) {
            errors.add(new WorkoutValidationException.StepValidationError(
                    index, "targetType", "Target type is required"));
            return null;
        }
        try {
            return TargetType.valueOf(step.targetType());
        } catch (IllegalArgumentException e) {
            errors.add(new WorkoutValidationException.StepValidationError(
                    index, "targetType", "Invalid target type: " + step.targetType()));
            return null;
        }
    }

    private Intensity parseIntensity(WorkoutStepRequest step, int index,
                                     List<WorkoutValidationException.StepValidationError> errors) {
        if (step.intensity() == null) {
            errors.add(new WorkoutValidationException.StepValidationError(
                    index, "intensity", "Intensity is required"));
            return null;
        }
        try {
            return Intensity.valueOf(step.intensity());
        } catch (IllegalArgumentException e) {
            errors.add(new WorkoutValidationException.StepValidationError(
                    index, "intensity", "Invalid intensity: " + step.intensity()));
            return null;
        }
    }

    private void validateDurationValue(WorkoutStepRequest step, int index, DurationType durationType,
                                       List<WorkoutValidationException.StepValidationError> errors) {
        Integer value = step.durationValue();

        switch (durationType) {
            case TIME -> {
                if (value == null || value < 1000 || value > 86_400_000) {
                    errors.add(new WorkoutValidationException.StepValidationError(
                            index, "durationValue",
                            "TIME duration value must be between 1000 and 86400000 milliseconds"));
                }
            }
            case DISTANCE -> {
                if (value == null || value < 1 || value > 100_000_000) {
                    errors.add(new WorkoutValidationException.StepValidationError(
                            index, "durationValue",
                            "DISTANCE duration value must be between 1 and 100000000"));
                }
            }
            case CALORIES -> {
                if (value == null || value < 1 || value > 10_000) {
                    errors.add(new WorkoutValidationException.StepValidationError(
                            index, "durationValue",
                            "CALORIES duration value must be between 1 and 10000"));
                }
            }
            case OPEN -> {
                // OPEN accepts null or zero
                if (value != null && value != 0) {
                    errors.add(new WorkoutValidationException.StepValidationError(
                            index, "durationValue",
                            "OPEN duration value must be null or zero"));
                }
            }
            case HR_LESS_THAN, HR_GREATER_THAN -> {
                if (value == null || !isValidHrValue(value)) {
                    errors.add(new WorkoutValidationException.StepValidationError(
                            index, "durationValue",
                            "HR duration value must be in range [0, 100] (percentage) or [101, 350] (absolute BPM + 100)"));
                }
            }
            case POWER_LESS_THAN, POWER_GREATER_THAN -> {
                if (value == null || !isValidPowerValue(value)) {
                    errors.add(new WorkoutValidationException.StepValidationError(
                            index, "durationValue",
                            "Power duration value must be in range [0, 1000] (percentage) or [1001, 2500] (absolute watts + 1000)"));
                }
            }
            default -> {
                // REPEAT_UNTIL_STEPS_COMPLETE handled separately
            }
        }
    }

    private void validateTargetValues(WorkoutStepRequest step, int index, TargetType targetType,
                                      List<WorkoutValidationException.StepValidationError> errors) {
        Integer low = step.targetValueLow();
        Integer high = step.targetValueHigh();

        switch (targetType) {
            case OPEN -> {
                // OPEN accepts null target values — no validation needed
            }
            case SPEED -> {
                validateRangedTarget(index, low, high, 1, 100_000,
                        "SPEED target value must be between 1 and 100000", errors);
            }
            case HEART_RATE -> {
                boolean lowValid = low != null && isValidHrValue(low);
                boolean highValid = high != null && isValidHrValue(high);

                if (!lowValid) {
                    errors.add(new WorkoutValidationException.StepValidationError(
                            index, "targetValueLow",
                            "HEART_RATE target value must be in range [0, 100] (percentage) or [101, 350] (absolute BPM + 100)"));
                }
                if (!highValid) {
                    errors.add(new WorkoutValidationException.StepValidationError(
                            index, "targetValueHigh",
                            "HEART_RATE target value must be in range [0, 100] (percentage) or [101, 350] (absolute BPM + 100)"));
                }
                if (lowValid && highValid && low > high) {
                    errors.add(new WorkoutValidationException.StepValidationError(
                            index, "targetValueLow",
                            "targetValueLow must be less than or equal to targetValueHigh"));
                }
            }
            case CADENCE -> {
                validateRangedTarget(index, low, high, 0, 255,
                        "CADENCE target value must be between 0 and 255", errors);
            }
            case POWER -> {
                boolean lowValid = low != null && isValidPowerValue(low);
                boolean highValid = high != null && isValidPowerValue(high);

                if (!lowValid) {
                    errors.add(new WorkoutValidationException.StepValidationError(
                            index, "targetValueLow",
                            "POWER target value must be in range [0, 1000] (percentage) or [1001, 2500] (absolute watts + 1000)"));
                }
                if (!highValid) {
                    errors.add(new WorkoutValidationException.StepValidationError(
                            index, "targetValueHigh",
                            "POWER target value must be in range [0, 1000] (percentage) or [1001, 2500] (absolute watts + 1000)"));
                }
                if (lowValid && highValid && low > high) {
                    errors.add(new WorkoutValidationException.StepValidationError(
                            index, "targetValueLow",
                            "targetValueLow must be less than or equal to targetValueHigh"));
                }
            }
        }
    }

    private void validateRangedTarget(int index, Integer low, Integer high,
                                      int min, int max, String rangeMessage,
                                      List<WorkoutValidationException.StepValidationError> errors) {
        boolean lowValid = low != null && low >= min && low <= max;
        boolean highValid = high != null && high >= min && high <= max;

        if (!lowValid) {
            errors.add(new WorkoutValidationException.StepValidationError(
                    index, "targetValueLow", rangeMessage));
        }
        if (!highValid) {
            errors.add(new WorkoutValidationException.StepValidationError(
                    index, "targetValueHigh", rangeMessage));
        }
        if (lowValid && highValid && low > high) {
            errors.add(new WorkoutValidationException.StepValidationError(
                    index, "targetValueLow",
                    "targetValueLow must be less than or equal to targetValueHigh"));
        }
    }

    private void validateRepeatStep(WorkoutStepRequest step, int index, List<WorkoutStepRequest> steps,
                                    Intensity intensity, TargetType targetType,
                                    List<WorkoutValidationException.StepValidationError> errors) {
        // Intensity must be REST
        if (intensity != null && intensity != Intensity.REST) {
            errors.add(new WorkoutValidationException.StepValidationError(
                    index, "intensity",
                    "Repeat step intensity must be REST"));
        }

        // TargetType must be OPEN
        if (targetType != null && targetType != TargetType.OPEN) {
            errors.add(new WorkoutValidationException.StepValidationError(
                    index, "targetType",
                    "Repeat step targetType must be OPEN"));
        }

        // durationValue must reference a valid preceding step index
        Integer durationValue = step.durationValue();
        if (durationValue == null || durationValue < 0 || durationValue >= index) {
            errors.add(new WorkoutValidationException.StepValidationError(
                    index, "durationValue",
                    "Repeat step durationValue must reference a valid preceding step index (>= 0 and < current index)"));
        }

        // targetValueLow (repetitions) must be in [1, 100]
        Integer repetitions = step.targetValueLow();
        if (repetitions == null || repetitions < 1 || repetitions > 100) {
            errors.add(new WorkoutValidationException.StepValidationError(
                    index, "targetValueLow",
                    "Repeat step repetitions must be between 1 and 100"));
        }

        // Detect nested repeats: range between durationValue and current index must not contain another REPEAT
        if (durationValue != null && durationValue >= 0 && durationValue < index) {
            for (int j = durationValue; j < index; j++) {
                WorkoutStepRequest innerStep = steps.get(j);
                if (DurationType.REPEAT_UNTIL_STEPS_COMPLETE.name().equals(innerStep.durationType())) {
                    errors.add(new WorkoutValidationException.StepValidationError(
                            index, "durationValue",
                            "Nested repeats are not supported"));
                    break;
                }
            }
        }
    }

    /**
     * Validates HR value encoding: [0, 100] for percentage of max HR,
     * or [101, 350] for absolute BPM (stored as BPM + 100).
     */
    private boolean isValidHrValue(int value) {
        return (value >= 0 && value <= 100) || (value >= 101 && value <= 350);
    }

    /**
     * Validates Power value encoding: [0, 1000] for percentage of FTP,
     * or [1001, 2500] for absolute watts (stored as watts + 1000).
     */
    private boolean isValidPowerValue(int value) {
        return (value >= 0 && value <= 1000) || (value >= 1001 && value <= 2500);
    }
}
