package com.trainer.profile;

import java.util.List;

/**
 * Exception thrown when cross-field validation fails on a profile request.
 * Carries a list of field-level validation errors so that all issues
 * can be reported to the client in a single 400 response.
 */
public class ProfileValidationException extends RuntimeException {

    private final List<FieldValidationError> errors;

    public ProfileValidationException(List<FieldValidationError> errors) {
        super("Profile validation failed");
        this.errors = List.copyOf(errors);
    }

    public List<FieldValidationError> getErrors() {
        return errors;
    }

    public record FieldValidationError(String field, String message) {
    }
}
