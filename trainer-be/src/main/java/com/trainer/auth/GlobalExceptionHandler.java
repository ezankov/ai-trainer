package com.trainer.auth;

import com.trainer.ai.AiGenerationException;
import com.trainer.ai.AiGenerationTimeoutException;
import com.trainer.ai.AiModelNotAvailableException;
import com.trainer.ai.AiModelNotSupportedException;
import com.trainer.ai.AiResponseParseException;
import com.trainer.ai.AiResponseValidationException;
import com.trainer.ai.AthleteProfileNotFoundException;
import com.trainer.profile.ProfileAlreadyExistsException;
import com.trainer.profile.ProfileNotFoundException;
import com.trainer.profile.ProfileValidationException;
import com.trainer.trainingplan.InvalidStateTransitionException;
import com.trainer.trainingplan.PlanSchedulingException;
import com.trainer.trainingplan.TrainingPlanNotFoundException;
import com.trainer.workout.WorkoutNotFoundException;
import com.trainer.workout.WorkoutValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(TrainingPlanNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleTrainingPlanNotFound(TrainingPlanNotFoundException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidStateTransition(InvalidStateTransitionException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(PlanSchedulingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handlePlanScheduling(PlanSchedulingException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler({BadCredentialsException.class, DisabledException.class, UsernameNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationFailure(Exception ex) {
        return new ErrorResponse("Invalid credentials");
    }

    @ExceptionHandler(AiModelNotAvailableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleAiModelNotAvailable(AiModelNotAvailableException ex) {
        log.error("AI model not available: {}", ex.getMessage(), ex);
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(AiModelNotSupportedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleAiModelNotSupported(AiModelNotSupportedException ex) {
        log.error("AI model not supported: {}", ex.getMessage(), ex);
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(AthleteProfileNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleAthleteProfileNotFound(AthleteProfileNotFoundException ex) {
        log.error("Athlete profile not found: {}", ex.getMessage(), ex);
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(AiGenerationException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse handleAiGeneration(AiGenerationException ex) {
        log.error("AI generation failed: {}", ex.getMessage(), ex);
        return new ErrorResponse("AI model failed to generate the plan");
    }

    @ExceptionHandler(AiGenerationTimeoutException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    public ErrorResponse handleAiGenerationTimeout(AiGenerationTimeoutException ex) {
        log.error("AI generation timed out: {}", ex.getMessage(), ex);
        return new ErrorResponse("AI model timed out while generating the plan");
    }

    @ExceptionHandler(AiResponseValidationException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse handleAiResponseValidation(AiResponseValidationException ex) {
        log.error("AI response validation failed: {}", ex.getMessage(), ex);
        return new ErrorResponse("AI model returned an invalid response");
    }

    @ExceptionHandler(AiResponseParseException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ErrorResponse handleAiResponseParse(AiResponseParseException ex) {
        log.error("AI response parse failed: {}", ex.getMessage(), ex);
        return new ErrorResponse("AI model returned an invalid response");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex) {
        return new ErrorResponse("An unexpected error occurred");
    }
}
