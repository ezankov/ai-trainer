package com.trainer.ai;

/**
 * Thrown when the AI provider returns an HTTP error (4xx or 5xx) during plan generation.
 * Maps to HTTP 502 Bad Gateway.
 */
public class AiGenerationException extends AiException {

    private final Integer httpStatusCode;

    public AiGenerationException(String message) {
        super(message);
        this.httpStatusCode = null;
    }

    public AiGenerationException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = null;
    }

    public AiGenerationException(String message, int httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    public AiGenerationException(String message, int httpStatusCode, Throwable cause) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * Returns the HTTP status code from the AI provider, or null if not available.
     */
    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }
}
