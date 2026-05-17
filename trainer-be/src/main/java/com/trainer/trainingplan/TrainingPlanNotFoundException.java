package com.trainer.trainingplan;

/**
 * Thrown when a training plan cannot be found (either does not exist or is not owned by the requesting user).
 * Maps to HTTP 404.
 */
public class TrainingPlanNotFoundException extends RuntimeException {

    public TrainingPlanNotFoundException(String message) {
        super(message);
    }
}
