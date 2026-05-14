package com.trainer.profile;

/**
 * Thrown when attempting to create an athlete profile for a user who already has one.
 * Results in HTTP 409 Conflict.
 */
public class ProfileAlreadyExistsException extends RuntimeException {

    public ProfileAlreadyExistsException() {
        super("Athlete profile already exists");
    }
}
