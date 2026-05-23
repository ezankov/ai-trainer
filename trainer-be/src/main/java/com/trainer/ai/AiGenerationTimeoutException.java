package com.trainer.ai;

/**
 * Thrown when the AI model API does not respond within the configured timeout (60 seconds).
 * Maps to HTTP 504 Gateway Timeout.
 */
public class AiGenerationTimeoutException extends AiException {

    public AiGenerationTimeoutException(String message) {
        super(message);
    }

    public AiGenerationTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
