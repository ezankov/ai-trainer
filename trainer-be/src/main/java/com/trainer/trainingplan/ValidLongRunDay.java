package com.trainer.trainingplan;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Cross-field validation: longRunDay must be present in the trainingDays list.
 * Applied at the record/class level.
 */
@Documented
@Constraint(validatedBy = LongRunDayValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLongRunDay {
    String message() default "Long run day must be one of the selected training days";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
