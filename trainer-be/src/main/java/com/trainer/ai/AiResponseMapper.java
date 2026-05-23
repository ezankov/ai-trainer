package com.trainer.ai;

import com.trainer.trainingplan.PlanWorkout;
import com.trainer.trainingplan.PlanWorkoutRepository;
import com.trainer.trainingplan.TrainingPlan;
import com.trainer.workout.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps a validated {@link AiPlanResponse} to {@link Workout} and {@link PlanWorkout}
 * entities and persists them via their respective repositories.
 */
@Component
public class AiResponseMapper {

    private final WorkoutRepository workoutRepository;
    private final PlanWorkoutRepository planWorkoutRepository;

    public AiResponseMapper(WorkoutRepository workoutRepository,
                            PlanWorkoutRepository planWorkoutRepository) {
        this.workoutRepository = workoutRepository;
        this.planWorkoutRepository = planWorkoutRepository;
    }

    /**
     * Maps the validated AI response to Workout and PlanWorkout entities
     * and persists them.
     *
     * @param response the validated AI plan response
     * @param plan     the training plan to associate workouts with
     */
    public void mapAndPersist(AiPlanResponse response, TrainingPlan plan) {
        for (AiWorkoutResponse workoutResponse : response.workouts()) {
            Workout workout = createWorkout(workoutResponse, plan);
            List<WorkoutStep> steps = createSteps(workoutResponse.steps(), workout);
            workout.setSteps(steps);

            Workout savedWorkout = workoutRepository.save(workout);

            PlanWorkout planWorkout = createPlanWorkout(workoutResponse.schedule(), plan, savedWorkout);
            planWorkoutRepository.save(planWorkout);
        }
    }

    private Workout createWorkout(AiWorkoutResponse workoutResponse, TrainingPlan plan) {
        Workout workout = new Workout();
        workout.setUserId(plan.getUserId());
        workout.setName(workoutResponse.name());
        workout.setSportType(SportType.valueOf(workoutResponse.sportType()));
        workout.setSubSport(workoutResponse.subSport() != null
                ? SubSport.valueOf(workoutResponse.subSport())
                : null);
        workout.setNumValidSteps(workoutResponse.steps().size());
        return workout;
    }

    private List<WorkoutStep> createSteps(List<AiWorkoutStepResponse> stepResponses, Workout workout) {
        List<WorkoutStep> steps = new ArrayList<>();
        for (AiWorkoutStepResponse stepResponse : stepResponses) {
            WorkoutStep step = new WorkoutStep();
            step.setWorkout(workout);
            step.setStepOrder(stepResponse.stepOrder());
            step.setStepName(stepResponse.stepName());
            step.setIntensity(Intensity.valueOf(stepResponse.intensity()));
            step.setDurationType(DurationType.valueOf(stepResponse.durationType()));
            step.setDurationValue(stepResponse.durationValue());
            step.setTargetType(TargetType.valueOf(stepResponse.targetType()));
            step.setTargetValueLow(stepResponse.targetValueLow());
            step.setTargetValueHigh(stepResponse.targetValueHigh());
            steps.add(step);
        }
        return steps;
    }

    private PlanWorkout createPlanWorkout(AiScheduleResponse schedule, TrainingPlan plan, Workout workout) {
        PlanWorkout planWorkout = new PlanWorkout();
        planWorkout.setTrainingPlan(plan);
        planWorkout.setWorkout(workout);
        planWorkout.setWeekNumber(schedule.weekNumber());
        planWorkout.setDayOfWeek(schedule.dayOfWeek());
        planWorkout.setOrderInDay(schedule.orderInDay());
        return planWorkout;
    }
}
