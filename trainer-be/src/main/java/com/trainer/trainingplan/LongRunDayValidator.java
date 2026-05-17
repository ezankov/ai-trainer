package com.trainer.trainingplan;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates that the longRunDay field value is contained within the trainingDays list.
 * This is a cross-field validator applied at the record level.
 */
public class LongRunDayValidator implements ConstraintValidator<ValidLongRunDay, CreateTrainingPlanRequest> {

    @Override
    public boolean isValid(CreateTrainingPlanRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }
        if (request.longRunDay() == null || request.trainingDays() == null || request.trainingDays().isEmpty()) {
            // Other validators handle null/empty checks
            return true;
        }
        boolean valid = request.trainingDays().contains(request.longRunDay());
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Long run day must be one of the selected training days"
            ).addPropertyNode("longRunDay").addConstraintViolation();
        }
        return valid;
    }
}
