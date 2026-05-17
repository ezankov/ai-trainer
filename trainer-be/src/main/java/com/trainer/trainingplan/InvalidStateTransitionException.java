package com.trainer.trainingplan;

/**
 * Thrown when a state transition is not allowed from the plan's current state.
 * Maps to HTTP 400.
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
