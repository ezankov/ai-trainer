package com.trainer.ai;

/**
 * Thrown when a user does not have an athlete profile but one is required
 * as a pre-condition for AI plan generation.
 * Maps to HTTP 400 Bad Request.
 */
public class AthleteProfileNotFoundException extends RuntimeException {

    public AthleteProfileNotFoundException(String message) {
        super(message);
    }
}
