package com.trainer.ai;

/**
 * Thrown when no AI plan generator implementation is registered for the requested model.
 * Maps to HTTP 400 Bad Request.
 */
public class AiModelNotSupportedException extends AiException {

    public AiModelNotSupportedException(String message) {
        super(message);
    }
}
