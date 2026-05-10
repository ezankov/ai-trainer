package com.trainer.auth;

/**
 * Thrown when a registration request uses a username that is already in use.
 * Maps to HTTP 409 Conflict.
 */
public class UsernameAlreadyTakenException extends RuntimeException {

    public UsernameAlreadyTakenException(String message) {
        super(message);
    }
}
