package com.trainer.trainingplan;

/**
 * Thrown when a workout scheduling operation violates plan constraints.
 * Maps to HTTP 400.
 */
public class PlanSchedulingException extends RuntimeException {

    public PlanSchedulingException(String message) {
        super(message);
    }
}
