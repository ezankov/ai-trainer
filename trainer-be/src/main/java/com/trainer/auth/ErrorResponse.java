package com.trainer.auth;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String message,
        String field
) {
    /** Convenience constructor for errors without a specific field. */
    public ErrorResponse(String message) {
        this(message, null);
    }
}
