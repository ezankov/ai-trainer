package com.trainer.profile;

/**
 * Thrown when an athlete profile is not found for the given user.
 * Results in HTTP 404 Not Found.
 */
public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException() {
        super("Athlete profile not found");
    }
}
