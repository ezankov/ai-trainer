package com.trainer.ai;

/**
 * Thrown when the AI model response cannot be parsed as JSON or does not
 * conform to the expected response schema.
 * Maps to HTTP 502 Bad Gateway.
 */
public class AiResponseParseException extends AiException {

    public AiResponseParseException(String message) {
        super(message);
    }

    public AiResponseParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
