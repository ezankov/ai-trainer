package com.trainer.workout;

import java.util.List;

/**
 * Exception thrown when workout step validation fails.
 * Carries a list of step-level validation errors so that all issues
 * can be reported to the client in a single 400 response.
 */
public class WorkoutValidationException extends RuntimeException {

    private final List<StepValidationError> errors;

    public WorkoutValidationException(List<StepValidationError> errors) {
        super("Workout step validation failed");
        this.errors = List.copyOf(errors);
    }

    public List<StepValidationError> getErrors() {
        return errors;
    }

    /**
     * Represents a single validation error on a specific step field.
     *
     * @param stepIndex zero-based index of the step that failed validation
     * @param field     the field name that failed validation
     * @param message   human-readable error description
     */
    public record StepValidationError(int stepIndex, String field, String message) {
    }
}
