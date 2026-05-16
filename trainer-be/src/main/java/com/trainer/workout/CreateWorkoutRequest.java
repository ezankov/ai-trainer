package com.trainer.workout;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO for creating a new workout via POST /api/workouts.
 */
public record CreateWorkoutRequest(

        @NotBlank
        @Size(max = 50)
        String name,

        @NotNull
        String sportType,

        String subSport,

        @NotEmpty
        @Size(max = 50)
        @Valid
        List<WorkoutStepRequest> steps
) {
}
