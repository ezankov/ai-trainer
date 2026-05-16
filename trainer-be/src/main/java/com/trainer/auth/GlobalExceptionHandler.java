package com.trainer.auth;

import com.trainer.profile.ProfileAlreadyExistsException;
import com.trainer.profile.ProfileNotFoundException;
import com.trainer.profile.ProfileValidationException;
import com.trainer.workout.WorkoutNotFoundException;
import com.trainer.workout.WorkoutValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);

        if (fieldError != null) {
            return new ErrorResponse(fieldError.getDefaultMessage(), fieldError.getField());
        }
        return new ErrorResponse("Validation failed");
    }

    @ExceptionHandler(ProfileValidationException.class)
    public ResponseEntity<Map<String, Object>> handleProfileValidation(ProfileValidationException ex) {
        List<Map<String, String>> errors = ex.getErrors().stream()
                .map(e -> Map.of("field", e.field(), "message", e.message()))
                .toList();

        Map<String, Object> body = Map.of(
                "message", "Validation failed",
                "errors", errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(UsernameAlreadyTakenException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUsernameAlreadyTaken(UsernameAlreadyTakenException ex) {
        return new ErrorResponse("Username already taken", "username");
    }

    @ExceptionHandler(EmailAlreadyTakenException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEmailAlreadyTaken(EmailAlreadyTakenException ex) {
        return new ErrorResponse("Email already taken", "email");
    }

    @ExceptionHandler(ProfileAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleProfileAlreadyExists(ProfileAlreadyExistsException ex) {
        return new ErrorResponse("Athlete profile already exists");
    }

    @ExceptionHandler(ProfileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleProfileNotFound(ProfileNotFoundException ex) {
        return new ErrorResponse("Athlete profile not found");
    }

    @ExceptionHandler(WorkoutNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleWorkoutNotFound(WorkoutNotFoundException ex) {
        return new ErrorResponse("Workout not found");
    }

    @ExceptionHandler(WorkoutValidationException.class)
    public ResponseEntity<Map<String, Object>> handleWorkoutValidation(WorkoutValidationException ex) {
        List<WorkoutValidationException.StepValidationError> errors = ex.getErrors();

        // Special case: workout-level validation error (stepIndex == -1) for invalid sportType/subSport
        if (errors.size() == 1 && errors.getFirst().stepIndex() == -1) {
            WorkoutValidationException.StepValidationError error = errors.getFirst();
            Map<String, Object> body = Map.of(
                    "message", "Invalid sport type",
                    "field", error.field()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }

        // Step-level validation errors
        List<Map<String, Object>> errorList = errors.stream()
                .map(e -> Map.<String, Object>of(
                        "stepIndex", e.stepIndex(),
                        "field", e.field(),
                        "message", e.message()
                ))
                .toList();

        Map<String, Object> body = Map.of(
                "message", "Workout step validation failed",
                "errors", errorList
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        if (UUID.class.equals(ex.getRequiredType())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Invalid identifier format"));
        }
        // For non-UUID type mismatches, return a generic bad request
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Invalid parameter format"));
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class, UsernameNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationFailure(Exception ex) {
        return new ErrorResponse("Invalid credentials");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex) {
        return new ErrorResponse("An unexpected error occurred");
    }
}
