package com.trainer.auth;

/**
 * Thrown when a registration request uses an email address that is already in use.
 * Maps to HTTP 409 Conflict.
 */
public class EmailAlreadyTakenException extends RuntimeException {

    public EmailAlreadyTakenException(String message) {
        super(message);
    }
}
