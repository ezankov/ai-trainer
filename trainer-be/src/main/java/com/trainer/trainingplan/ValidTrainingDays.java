package com.trainer.trainingplan;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that a list of training days contains only values 1–7 with no duplicates.
 */
@Documented
@Constraint(validatedBy = TrainingDaysValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTrainingDays {
    String message() default "Training days must contain 1–7 unique values between 1 (Monday) and 7 (Sunday)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
