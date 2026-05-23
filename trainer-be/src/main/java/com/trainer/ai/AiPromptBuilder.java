package com.trainer.ai;

import com.trainer.trainingplan.PlanDistance;
import com.trainer.trainingplan.PlanDuration;
import com.trainer.trainingplan.TrainingPlan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class AiPromptBuilder {

    private final String trainingPrinciples;

    public AiPromptBuilder(@Value("classpath:prompts/training-principles.md") Resource principlesResource) {
        try {
            this.trainingPrinciples = principlesResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load training principles from classpath", e);
        }
    }

    public String buildSystemPrompt() {
        return """
                You are an expert running coach and training plan generator.

                %s

                Before generating the plan, call the `getAthleteProfile` tool to retrieve the athlete's fitness data.

                Respond with a JSON object in the following format:
                {
                  "workouts": [
                    {
                      "name": "string (max 50 chars, descriptive e.g. 'Easy Run 45min')",
                      "sportType": "RUNNING",
                      "subSport": "string or null",
                      "steps": [
                        {
                          "stepOrder": integer (1-based sequential),
                          "stepName": "string (max 50 chars)",
                          "intensity": "WARMUP|ACTIVE|INTERVAL|REST|RECOVERY|COOLDOWN",
                          "durationType": "TIME|DISTANCE|REPEAT_UNTIL_STEPS_COMPLETE",
                          "durationValue": integer (seconds for TIME, metres for DISTANCE),
                          "targetType": "SPEED|HEART_RATE|CADENCE|POWER|OPEN",
                          "targetValueLow": integer or null,
                          "targetValueHigh": integer or null
                        }
                      ],
                      "schedule": {
                        "weekNumber": integer (1 to plan duration weeks),
                        "dayOfWeek": integer (1=Monday to 7=Sunday),
                        "orderInDay": 1
                      }
                    }
                  ]
                }

                Respond ONLY with the JSON object. Do not include any other text."""
                .formatted(trainingPrinciples);
    }

    public String buildUserPrompt(TrainingPlan plan) {
        String distanceText = toHumanReadableDistance(plan.getDistance());
        String durationText = plan.getDuration().getWeeks() + " weeks";
        String raceDateText = plan.getRaceDate().toString();
        String paceText = formatPace(plan.getTargetPaceSecondsPerKm());
        String trainingDaysText = toTrainingDayNames(plan.getTrainingDays());
        String longRunDayName = toDayName(plan.getLongRunDay());
        int totalWeeks = plan.getDuration().getWeeks();

        return """
                Generate a training plan with the following parameters:
                - Event: %s
                - Distance: %s
                - Duration: %s
                - Race date: %s
                - Target pace: %s
                - Training days: %s
                - Long run day: %s

                Generate exactly one workout per training day per week, covering all %d weeks.
                Assign the long run to %s each week."""
                .formatted(
                        plan.getEventName(),
                        distanceText,
                        durationText,
                        raceDateText,
                        paceText,
                        trainingDaysText,
                        longRunDayName,
                        totalWeeks,
                        longRunDayName
                );
    }

    String toHumanReadableDistance(PlanDistance distance) {
        return switch (distance) {
            case FIVE_K -> "5K";
            case TEN_K -> "10K";
            case HALF_MARATHON -> "Half Marathon";
            case MARATHON -> "Marathon";
        };
    }

    String formatPace(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return "%d:%02d/km".formatted(minutes, seconds);
    }

    String toDayName(int dayOfWeekIso) {
        return DayOfWeek.of(dayOfWeekIso)
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    String toTrainingDayNames(List<Integer> days) {
        return days.stream()
                .map(this::toDayName)
                .collect(Collectors.joining(", "));
    }
}
