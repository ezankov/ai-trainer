package com.trainer.trainingplan;

import com.trainer.auth.ErrorResponse;
import com.trainer.auth.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for training plan CRUD and state lifecycle operations.
 * All endpoints require authentication — the user ID is extracted from the SecurityContext.
 */
@RestController
@RequestMapping("/api/training-plans")
public class TrainingPlanController {

    private final TrainingPlanService trainingPlanService;

    public TrainingPlanController(TrainingPlanService trainingPlanService) {
        this.trainingPlanService = trainingPlanService;
    }

    @PostMapping
    public ResponseEntity<TrainingPlanResponse> createPlan(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateTrainingPlanRequest request) {
        TrainingPlanResponse response = trainingPlanService.createPlan(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<TrainingPlanSummaryResponse> getPlans(@AuthenticationPrincipal User user) {
        return trainingPlanService.getPlans(user.getId());
    }

    @GetMapping("/active")
    public TrainingPlanDetailResponse getActivePlan(@AuthenticationPrincipal User user) {
        return trainingPlanService.getActivePlan(user.getId());
    }

    @GetMapping("/{id}")
    public TrainingPlanDetailResponse getPlan(
            @AuthenticationPrincipal User user,
            @PathVariable("id") UUID id) {
        return trainingPlanService.getPlan(user.getId(), id);
    }

    @PutMapping("/{id}/activate")
    public TrainingPlanResponse activatePlan(
            @AuthenticationPrincipal User user,
            @PathVariable("id") UUID id) {
        return trainingPlanService.activatePlan(user.getId(), id);
    }

    @PutMapping("/{id}/complete")
    public TrainingPlanResponse completePlan(
            @AuthenticationPrincipal User user,
            @PathVariable("id") UUID id) {
        return trainingPlanService.completePlan(user.getId(), id);
    }

    @PutMapping("/{id}/terminate")
    public TrainingPlanResponse terminatePlan(
            @AuthenticationPrincipal User user,
            @PathVariable("id") UUID id) {
        return trainingPlanService.terminatePlan(user.getId(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(
            @AuthenticationPrincipal User user,
            @PathVariable("id") UUID id) {
        trainingPlanService.deletePlan(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Handles invalid UUID path variables for this controller by returning 404,
     * treating them the same as "not found" to avoid leaking ID format information.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUuid(MethodArgumentTypeMismatchException ex) {
        if (UUID.class.equals(ex.getRequiredType())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Training plan not found"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Invalid parameter format"));
    }
}
