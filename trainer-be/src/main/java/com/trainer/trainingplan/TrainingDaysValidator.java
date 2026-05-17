package com.trainer.trainingplan;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.List;

/**
 * Validates that a list of integers represents valid training days:
 * each value must be between 1 and 7 (inclusive) with no duplicates.
 */
public class TrainingDaysValidator implements ConstraintValidator<ValidTrainingDays, List<Integer>> {

    @Override
    public boolean isValid(List<Integer> value, ConstraintValidatorContext context) {
        if (value == null) {
            // @NotEmpty handles null/empty check separately
            return true;
        }
        if (value.isEmpty()) {
            return true;
        }
        // Check for values outside 1-7
        for (Integer day : value) {
            if (day == null || day < 1 || day > 7) {
                return false;
            }
        }
        // Check for duplicates
        return new HashSet<>(value).size() == value.size();
    }
}
