package com.trainer.trainingplan;

import com.trainer.ai.AiPlanGenerator;
import com.trainer.workout.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates placeholder workouts and schedules them into a training plan.
 * Used when aiModel = DUMMY to provide sample data for UI validation
 * without calling a real AI service.
 */
@Component
public class DummyPlanGenerator implements AiPlanGenerator {

    private final WorkoutRepository workoutRepository;
    private final PlanWorkoutRepository planWorkoutRepository;

    public DummyPlanGenerator(WorkoutRepository workoutRepository,
                              PlanWorkoutRepository planWorkoutRepository) {
        this.workoutRepository = workoutRepository;
        this.planWorkoutRepository = planWorkoutRepository;
    }

    /**
     * Generates dummy workouts and assigns them to the plan based on its
     * duration (weeks) and training days configuration.
     * <p>
     * For each week, assigns one workout per training day:
     * - Long run day gets a "Long Run" workout
     * - Other training days get a rotation of Easy Run, Intervals, Tempo Run
     */
    @Override
    public void generate(TrainingPlan plan) {
        Long userId = plan.getUserId();
        int totalWeeks = plan.getDuration().getWeeks();
        List<Integer> trainingDays = plan.getTrainingDays();
        int longRunDay = plan.getLongRunDay();

        // Create the template workouts for this user
        Workout easyRun = createWorkout(userId, "Easy Run", 3, buildEasyRunSteps());
        Workout intervals = createWorkout(userId, "Intervals", 5, buildIntervalSteps());
        Workout tempoRun = createWorkout(userId, "Tempo Run", 3, buildTempoSteps());
        Workout longRun = createWorkout(userId, "Long Run", 3, buildLongRunSteps());

        // Non-long-run training days for rotation
        List<Integer> regularDays = trainingDays.stream()
                .filter(d -> d != longRunDay)
                .sorted()
                .toList();

        Workout[] rotation = {easyRun, intervals, tempoRun};

        // Assign workouts to each week
        List<PlanWorkout> assignments = new ArrayList<>();
        int rotationIndex = 0;

        for (int week = 1; week <= totalWeeks; week++) {
            // Assign long run
            PlanWorkout longRunAssignment = new PlanWorkout();
            longRunAssignment.setTrainingPlan(plan);
            longRunAssignment.setWorkout(longRun);
            longRunAssignment.setWeekNumber(week);
            longRunAssignment.setDayOfWeek(longRunDay);
            longRunAssignment.setOrderInDay(1);
            assignments.add(longRunAssignment);

            // Assign regular workouts
            for (Integer day : regularDays) {
                Workout workout = rotation[rotationIndex % rotation.length];
                rotationIndex++;

                PlanWorkout pw = new PlanWorkout();
                pw.setTrainingPlan(plan);
                pw.setWorkout(workout);
                pw.setWeekNumber(week);
                pw.setDayOfWeek(day);
                pw.setOrderInDay(1);
                assignments.add(pw);
            }
        }

        planWorkoutRepository.saveAll(assignments);
    }

    private Workout createWorkout(Long userId, String name, int numSteps, List<StepTemplate> steps) {
        Workout workout = new Workout();
        workout.setUserId(userId);
        workout.setName(name);
        workout.setSportType(SportType.RUNNING);
        workout.setSubSport(null);
        workout.setNumValidSteps(numSteps);

        List<WorkoutStep> workoutSteps = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            StepTemplate t = steps.get(i);
            WorkoutStep step = new WorkoutStep();
            step.setWorkout(workout);
            step.setStepOrder(i + 1);
            step.setStepName(t.name);
            step.setIntensity(t.intensity);
            step.setDurationType(t.durationType);
            step.setDurationValue(t.durationValue);
            step.setTargetType(t.targetType);
            step.setTargetValueLow(t.targetLow);
            step.setTargetValueHigh(t.targetHigh);
            workoutSteps.add(step);
        }
        workout.setSteps(workoutSteps);

        return workoutRepository.save(workout);
    }

    // -------------------------------------------------------------------------
    // Step templates for each workout type
    // -------------------------------------------------------------------------

    private List<StepTemplate> buildEasyRunSteps() {
        return List.of(
                new StepTemplate("Warm Up", Intensity.WARMUP, DurationType.TIME, 600, TargetType.OPEN, null, null),
                new StepTemplate("Easy Run", Intensity.ACTIVE, DurationType.TIME, 1800, TargetType.HEART_RATE, 120, 145),
                new StepTemplate("Cool Down", Intensity.COOLDOWN, DurationType.TIME, 300, TargetType.OPEN, null, null)
        );
    }

    private List<StepTemplate> buildIntervalSteps() {
        // Garmin FIT-compatible flat interval structure:
        // Step 0: Warm Up (2.4km, no target)
        // Step 1: Fast 1km (interval, pace target)
        // Step 2: Fast 200m (interval, pace target)
        // Step 3: Rest 2min (recovery, no target)
        // Step 4: Repeat from step 1, 4 iterations
        // Step 5: Cool Down (1.8km, no target)
        //
        // The repeat step (4) references durationValue=1 (step index to repeat from)
        // and targetValueLow=4 (number of iterations).
        // Steps 1-3 are the repeated block.
        return List.of(
                new StepTemplate("Warm Up", Intensity.WARMUP, DurationType.DISTANCE, 2400, TargetType.OPEN, null, null),
                new StepTemplate("Fast 1km", Intensity.INTERVAL, DurationType.DISTANCE, 1000, TargetType.SPEED, 210, 230),
                new StepTemplate("Fast 200m", Intensity.INTERVAL, DurationType.DISTANCE, 200, TargetType.SPEED, 190, 210),
                new StepTemplate("Rest", Intensity.REST, DurationType.TIME, 120, TargetType.OPEN, null, null),
                new StepTemplate("Repeat", Intensity.REST, DurationType.REPEAT_UNTIL_STEPS_COMPLETE, 1, TargetType.OPEN, 4, null),
                new StepTemplate("Cool Down", Intensity.COOLDOWN, DurationType.DISTANCE, 1800, TargetType.OPEN, null, null)
        );
    }

    private List<StepTemplate> buildTempoSteps() {
        return List.of(
                new StepTemplate("Warm Up", Intensity.WARMUP, DurationType.TIME, 600, TargetType.OPEN, null, null),
                new StepTemplate("Tempo", Intensity.ACTIVE, DurationType.TIME, 1200, TargetType.SPEED, 240, 270),
                new StepTemplate("Cool Down", Intensity.COOLDOWN, DurationType.TIME, 300, TargetType.OPEN, null, null)
        );
    }

    private List<StepTemplate> buildLongRunSteps() {
        return List.of(
                new StepTemplate("Warm Up", Intensity.WARMUP, DurationType.TIME, 600, TargetType.OPEN, null, null),
                new StepTemplate("Long Run", Intensity.ACTIVE, DurationType.TIME, 3600, TargetType.HEART_RATE, 130, 155),
                new StepTemplate("Cool Down", Intensity.COOLDOWN, DurationType.TIME, 300, TargetType.OPEN, null, null)
        );
    }

    private record StepTemplate(
            String name,
            Intensity intensity,
            DurationType durationType,
            Integer durationValue,
            TargetType targetType,
            Integer targetLow,
            Integer targetHigh
    ) {}
}
