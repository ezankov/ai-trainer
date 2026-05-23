package com.trainer.ai;

/**
 * Thrown when the requested AI model is disabled or its API key is not configured.
 * Maps to HTTP 400 Bad Request.
 */
public class AiModelNotAvailableException extends AiException {

    public AiModelNotAvailableException(String message) {
        super(message);
    }
}
