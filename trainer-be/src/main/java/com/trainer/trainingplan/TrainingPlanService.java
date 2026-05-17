package com.trainer.trainingplan;

import com.trainer.workout.Workout;
import com.trainer.workout.WorkoutRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for training plan business logic including CRUD operations
 * and state machine transitions.
 */
@Service
public class TrainingPlanService {

    private final TrainingPlanRepository trainingPlanRepository;
    private final PlanWorkoutRepository planWorkoutRepository;
    private final WorkoutRepository workoutRepository;
    private final DummyPlanGenerator dummyPlanGenerator;

    public TrainingPlanService(TrainingPlanRepository trainingPlanRepository,
                               PlanWorkoutRepository planWorkoutRepository,
                               WorkoutRepository workoutRepository,
                               DummyPlanGenerator dummyPlanGenerator) {
        this.trainingPlanRepository = trainingPlanRepository;
        this.planWorkoutRepository = planWorkoutRepository;
        this.workoutRepository = workoutRepository;
        this.dummyPlanGenerator = dummyPlanGenerator;
    }

    /**
     * Creates a new training plan with state NEW.
     * Validates enum values (distance, duration, aiModel) before persisting.
     * If aiModel is DUMMY, generates placeholder workouts and schedules them.
     */
    @Transactional
    public TrainingPlanResponse createPlan(Long userId, CreateTrainingPlanRequest request) {
        PlanDistance distance = parseEnum(PlanDistance.class, request.distance(), "distance");
        PlanDuration duration = parseEnum(PlanDuration.class, request.duration(), "duration");
        AiModel aiModel = parseEnum(AiModel.class, request.aiModel(), "aiModel");

        TrainingPlan plan = new TrainingPlan();
        plan.setUserId(userId);
        plan.setEventName(request.eventName());
        plan.setDistance(distance);
        plan.setDuration(duration);
        plan.setRaceDate(request.raceDate());
        plan.setTargetPaceSecondsPerKm(request.targetPaceSecondsPerKm());
        plan.setAiModel(aiModel);
        plan.setTrainingDays(request.trainingDays());
        plan.setLongRunDay(request.longRunDay());
        plan.setState(PlanState.NEW);

        TrainingPlan saved = trainingPlanRepository.save(plan);

        if (aiModel == AiModel.DUMMY) {
            dummyPlanGenerator.generate(saved);
        }

        return toResponse(saved);
    }

    /**
     * Returns all plans for a user ordered by createdAt descending.
     */
    public List<TrainingPlanSummaryResponse> getPlans(Long userId) {
        return trainingPlanRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /**
     * Returns a plan with workouts grouped by week.
     * Throws TrainingPlanNotFoundException if not found or not owned by the user.
     */
    public TrainingPlanDetailResponse getPlan(Long userId, UUID planId) {
        TrainingPlan plan = trainingPlanRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new TrainingPlanNotFoundException("Training plan not found"));
        return toDetailResponse(plan);
    }

    /**
     * Returns the active plan with workouts grouped by week.
     * Throws TrainingPlanNotFoundException if no active plan exists.
     */
    public TrainingPlanDetailResponse getActivePlan(Long userId) {
        TrainingPlan plan = trainingPlanRepository.findByUserIdAndState(userId, PlanState.ACTIVE)
                .orElseThrow(() -> new TrainingPlanNotFoundException("No active training plan found"));
        return toDetailResponse(plan);
    }

    /**
     * Activates a plan. If another plan is currently active, it is terminated first.
     * The target plan must be in state NEW or COMPLETED.
     * This method is transactional to ensure atomic terminate-then-activate.
     */
    @Transactional
    public TrainingPlanResponse activatePlan(Long userId, UUID planId) {
        TrainingPlan plan = trainingPlanRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new TrainingPlanNotFoundException("Training plan not found"));

        if (plan.getState() == PlanState.ACTIVE) {
            throw new InvalidStateTransitionException("Plan is already active");
        }
        if (plan.getState() == PlanState.TERMINATED) {
            throw new InvalidStateTransitionException("A terminated plan cannot be reactivated");
        }

        // Terminate current active plan if one exists
        trainingPlanRepository.findByUserIdAndState(userId, PlanState.ACTIVE)
                .ifPresent(activePlan -> {
                    activePlan.setState(PlanState.TERMINATED);
                    trainingPlanRepository.save(activePlan);
                });

        plan.setState(PlanState.ACTIVE);
        TrainingPlan saved = trainingPlanRepository.save(plan);
        return toResponse(saved);
    }

    /**
     * Completes a plan. Only ACTIVE plans can be completed.
     */
    public TrainingPlanResponse completePlan(Long userId, UUID planId) {
        TrainingPlan plan = trainingPlanRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new TrainingPlanNotFoundException("Training plan not found"));

        if (plan.getState() != PlanState.ACTIVE) {
            throw new InvalidStateTransitionException("Only active plans can be completed");
        }

        plan.setState(PlanState.COMPLETED);
        TrainingPlan saved = trainingPlanRepository.save(plan);
        return toResponse(saved);
    }

    /**
     * Terminates a plan. Only ACTIVE or NEW plans can be terminated.
     */
    public TrainingPlanResponse terminatePlan(Long userId, UUID planId) {
        TrainingPlan plan = trainingPlanRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new TrainingPlanNotFoundException("Training plan not found"));

        if (plan.getState() == PlanState.COMPLETED || plan.getState() == PlanState.TERMINATED) {
            throw new InvalidStateTransitionException("Plan cannot be terminated from its current state");
        }

        plan.setState(PlanState.TERMINATED);
        TrainingPlan saved = trainingPlanRepository.save(plan);
        return toResponse(saved);
    }

    /**
     * Deletes a plan if it is not ACTIVE. Cascades to PlanWorkouts.
     */
    public void deletePlan(Long userId, UUID planId) {
        TrainingPlan plan = trainingPlanRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new TrainingPlanNotFoundException("Training plan not found"));

        if (plan.getState() == PlanState.ACTIVE) {
            throw new InvalidStateTransitionException("An active plan must be terminated before deletion");
        }

        trainingPlanRepository.delete(plan);
    }

    /**
     * Assigns a workout to a specific position within a training plan's schedule.
     * Validates scheduling constraints: week range, day range, order range,
     * uniqueness, plan state, and workout existence.
     */
    @Transactional
    public void assignWorkout(Long userId, UUID planId, UUID workoutId, int weekNumber, int dayOfWeek, int orderInDay) {
        TrainingPlan plan = trainingPlanRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new TrainingPlanNotFoundException("Training plan not found"));

        // Validate plan state is NEW
        if (plan.getState() != PlanState.NEW) {
            throw new PlanSchedulingException("Workouts can only be assigned to plans in NEW state");
        }

        // Validate weekNumber
        int maxWeeks = plan.getDuration().getWeeks();
        if (weekNumber < 1 || weekNumber > maxWeeks) {
            throw new PlanSchedulingException(
                    "Week number must be between 1 and " + maxWeeks + " for this plan duration");
        }

        // Validate dayOfWeek
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new PlanSchedulingException("Day of week must be between 1 (Monday) and 7 (Sunday)");
        }

        // Validate orderInDay
        if (orderInDay < 1 || orderInDay > 5) {
            throw new PlanSchedulingException("Order in day must be between 1 and 5");
        }

        // Validate uniqueness of (weekNumber, dayOfWeek, orderInDay) within plan
        List<PlanWorkout> existingWorkouts = planWorkoutRepository
                .findByTrainingPlanIdOrderByWeekNumberAscDayOfWeekAscOrderInDayAsc(planId);
        boolean conflict = existingWorkouts.stream().anyMatch(pw ->
                pw.getWeekNumber().equals(weekNumber)
                        && pw.getDayOfWeek().equals(dayOfWeek)
                        && pw.getOrderInDay().equals(orderInDay));
        if (conflict) {
            throw new PlanSchedulingException("A workout is already scheduled at this position");
        }

        // Validate referenced workout exists
        Workout workout = workoutRepository.findById(workoutId)
                .orElseThrow(() -> new PlanSchedulingException("Referenced workout not found"));

        // Create and save the PlanWorkout
        PlanWorkout planWorkout = new PlanWorkout();
        planWorkout.setTrainingPlan(plan);
        planWorkout.setWorkout(workout);
        planWorkout.setWeekNumber(weekNumber);
        planWorkout.setDayOfWeek(dayOfWeek);
        planWorkout.setOrderInDay(orderInDay);

        planWorkoutRepository.save(planWorkout);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid value for " + fieldName + ": " + value);
        }
    }

    private TrainingPlanResponse toResponse(TrainingPlan plan) {
        return new TrainingPlanResponse(
                plan.getId(),
                plan.getEventName(),
                plan.getDistance().name(),
                plan.getDuration().name(),
                plan.getRaceDate(),
                plan.getTargetPaceSecondsPerKm(),
                plan.getAiModel().name(),
                plan.getTrainingDays(),
                plan.getLongRunDay(),
                plan.getState().name(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }

    private TrainingPlanSummaryResponse toSummaryResponse(TrainingPlan plan) {
        return new TrainingPlanSummaryResponse(
                plan.getId(),
                plan.getEventName(),
                plan.getDistance().name(),
                plan.getDuration().name(),
                plan.getRaceDate(),
                plan.getTargetPaceSecondsPerKm(),
                plan.getAiModel().name(),
                plan.getTrainingDays(),
                plan.getLongRunDay(),
                plan.getState().name(),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }

    private TrainingPlanDetailResponse toDetailResponse(TrainingPlan plan) {
        List<PlanWorkout> workouts = planWorkoutRepository
                .findByTrainingPlanIdOrderByWeekNumberAscDayOfWeekAscOrderInDayAsc(plan.getId());

        List<PlanWeekResponse> weeks = groupByWeek(workouts);

        return new TrainingPlanDetailResponse(
                plan.getId(),
                plan.getEventName(),
                plan.getDistance().name(),
                plan.getDuration().name(),
                plan.getRaceDate(),
                plan.getTargetPaceSecondsPerKm(),
                plan.getAiModel().name(),
                plan.getTrainingDays(),
                plan.getLongRunDay(),
                plan.getState().name(),
                plan.getCreatedAt(),
                plan.getUpdatedAt(),
                weeks
        );
    }

    private List<PlanWeekResponse> groupByWeek(List<PlanWorkout> workouts) {
        if (workouts.isEmpty()) {
            return List.of();
        }

        // Group by weekNumber preserving insertion order (already sorted by weekNumber ASC)
        Map<Integer, List<PlanWorkout>> grouped = workouts.stream()
                .collect(Collectors.groupingBy(
                        PlanWorkout::getWeekNumber,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.entrySet().stream()
                .map(entry -> new PlanWeekResponse(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(this::toWorkoutEntryResponse)
                                .toList()
                ))
                .toList();
    }

    private PlanWorkoutEntryResponse toWorkoutEntryResponse(PlanWorkout planWorkout) {
        var workout = planWorkout.getWorkout();
        var steps = workout.getSteps().stream()
                .map(step -> new WorkoutStepResponse(
                        step.getStepOrder(),
                        step.getStepName(),
                        step.getIntensity().name(),
                        step.getDurationType().name(),
                        step.getDurationValue(),
                        step.getTargetType().name(),
                        step.getTargetValueLow(),
                        step.getTargetValueHigh(),
                        step.getNotes()
                ))
                .toList();

        return new PlanWorkoutEntryResponse(
                planWorkout.getDayOfWeek(),
                planWorkout.getOrderInDay(),
                new WorkoutSummaryResponse(
                        workout.getId(),
                        workout.getName(),
                        workout.getSportType().name(),
                        workout.getSubSport() != null ? workout.getSubSport().name() : null,
                        workout.getNumValidSteps(),
                        steps
                )
        );
    }
}
