package com.trainer.workout;

/**
 * Thrown when a workout is not found or not owned by the requesting user.
 */
public class WorkoutNotFoundException extends RuntimeException {

    public WorkoutNotFoundException(String message) {
        super(message);
    }
}
