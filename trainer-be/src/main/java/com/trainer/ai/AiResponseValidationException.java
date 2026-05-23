package com.trainer.ai;

import java.util.List;

/**
 * Thrown when the AI model response fails validation (invalid enum values,
 * week numbers out of range, missing required fields, etc.).
 * Maps to HTTP 502 Bad Gateway.
 */
public class AiResponseValidationException extends AiException {

    private final List<String> validationErrors;

    public AiResponseValidationException(String message, List<String> validationErrors) {
        super(message);
        this.validationErrors = validationErrors != null ? List.copyOf(validationErrors) : List.of();
    }

    /**
     * Returns the list of validation error details describing which fields failed.
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
