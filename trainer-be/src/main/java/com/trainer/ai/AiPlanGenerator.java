package com.trainer.ai;

import com.trainer.trainingplan.TrainingPlan;

/**
 * Strategy interface for generating AI-powered training plan workouts.
 *
 * <p>Implementations of this interface are responsible for generating workouts
 * and scheduling them into the given training plan. The generation process
 * persists {@code Workout} and {@code PlanWorkout} entities as a side-effect.</p>
 *
 * <p>The provided {@code TrainingPlan} must already be saved (i.e., it must have
 * a non-null ID and userId). Implementations use the plan's parameters (event,
 * distance, duration, race date, target pace, training days, long run day) to
 * produce an appropriate set of workouts.</p>
 *
 * @see com.trainer.trainingplan.TrainingPlan
 */
public interface AiPlanGenerator {

    /**
     * Generates workouts and schedules them into the given training plan.
     * Persists {@code Workout} and {@code PlanWorkout} entities as a side-effect.
     *
     * <p>The plan parameter must have an ID and userId already set (it must be
     * a previously saved entity). Implementations will create workout entities
     * owned by the plan's user and link them to the plan via {@code PlanWorkout}
     * join entities with appropriate scheduling metadata.</p>
     *
     * @param plan the saved TrainingPlan entity (must have an ID and userId)
     * @throws AiGenerationException        if the AI model API fails or returns an HTTP error
     * @throws AiGenerationTimeoutException  if the AI model does not respond within the timeout period
     * @throws AiResponseValidationException if the AI model returns data that fails validation
     * @throws AiResponseParseException      if the AI model response cannot be parsed
     */
    void generate(TrainingPlan plan);
}
