package com.trainer.ai;

import com.trainer.trainingplan.AiModel;
import com.trainer.trainingplan.PlanDistance;
import com.trainer.trainingplan.PlanDuration;
import com.trainer.trainingplan.TrainingPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptBuilderTest {

    private AiPromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new AiPromptBuilder(new ClassPathResource("prompts/training-principles.md"));
    }

    @Test
    void buildSystemPrompt_containsRoleDefinition() {
        String prompt = promptBuilder.buildSystemPrompt();

        assertThat(prompt).contains("You are an expert running coach and training plan generator.");
    }

    @Test
    void buildSystemPrompt_containsTrainingPrinciples() {
        String prompt = promptBuilder.buildSystemPrompt();

        assertThat(prompt).contains("Training Plan Generation Principles");
        assertThat(prompt).contains("80/20 Rule");
        assertThat(prompt).contains("Periodization");
    }

    @Test
    void buildSystemPrompt_containsToolCallInstruction() {
        String prompt = promptBuilder.buildSystemPrompt();

        assertThat(prompt).contains("call the `getAthleteProfile` tool to retrieve the athlete's fitness data");
    }

    @Test
    void buildSystemPrompt_containsJsonResponseSchema() {
        String prompt = promptBuilder.buildSystemPrompt();

        assertThat(prompt).contains("\"workouts\"");
        assertThat(prompt).contains("\"sportType\"");
        assertThat(prompt).contains("\"steps\"");
        assertThat(prompt).contains("\"stepOrder\"");
        assertThat(prompt).contains("\"intensity\"");
        assertThat(prompt).contains("\"durationType\"");
        assertThat(prompt).contains("\"targetType\"");
        assertThat(prompt).contains("\"schedule\"");
        assertThat(prompt).contains("\"weekNumber\"");
        assertThat(prompt).contains("\"dayOfWeek\"");
        assertThat(prompt).contains("\"orderInDay\"");
    }

    @Test
    void buildSystemPrompt_containsJsonOnlyInstruction() {
        String prompt = promptBuilder.buildSystemPrompt();

        assertThat(prompt).contains("Respond ONLY with the JSON object. Do not include any other text.");
    }

    @Test
    void buildUserPrompt_containsEventName() {
        TrainingPlan plan = createTestPlan();

        String prompt = promptBuilder.buildUserPrompt(plan);

        assertThat(prompt).contains("Spring Marathon 2025");
    }

    @Test
    void buildUserPrompt_containsHumanReadableDistance() {
        TrainingPlan plan = createTestPlan();
        plan.setDistance(PlanDistance.HALF_MARATHON);

        String prompt = promptBuilder.buildUserPrompt(plan);

        assertThat(prompt).contains("Half Marathon");
    }

    @Test
    void buildUserPrompt_containsDurationInWeeks() {
        TrainingPlan plan = createTestPlan();
        plan.setDuration(PlanDuration.WEEKS_12);

        String prompt = promptBuilder.buildUserPrompt(plan);

        assertThat(prompt).contains("12 weeks");
    }

    @Test
    void buildUserPrompt_containsRaceDate() {
        TrainingPlan plan = createTestPlan();

        String prompt = promptBuilder.buildUserPrompt(plan);

        assertThat(prompt).contains("2025-09-15");
    }

    @Test
    void buildUserPrompt_containsFormattedTargetPace() {
        TrainingPlan plan = createTestPlan();
        plan.setTargetPaceSecondsPerKm(300);

        String prompt = promptBuilder.buildUserPrompt(plan);

        assertThat(prompt).contains("5:00/km");
    }

    @Test
    void buildUserPrompt_containsTrainingDayNames() {
        TrainingPlan plan = createTestPlan();
        plan.setTrainingDays(List.of(1, 3, 5, 7));

        String prompt = promptBuilder.buildUserPrompt(plan);

        assertThat(prompt).contains("Monday, Wednesday, Friday, Sunday");
    }

    @Test
    void buildUserPrompt_containsLongRunDayName() {
        TrainingPlan plan = createTestPlan();
        plan.setLongRunDay(7);

        String prompt = promptBuilder.buildUserPrompt(plan);

        assertThat(prompt).contains("Long run day: Sunday");
    }

    @Test
    void buildUserPrompt_containsWorkoutPerDayPerWeekInstruction() {
        TrainingPlan plan = createTestPlan();
        plan.setDuration(PlanDuration.WEEKS_10);

        String prompt = promptBuilder.buildUserPrompt(plan);

        assertThat(prompt).contains("Generate exactly one workout per training day per week, covering all 10 weeks.");
    }

    @Test
    void buildUserPrompt_containsLongRunAssignmentInstruction() {
        TrainingPlan plan = createTestPlan();
        plan.setLongRunDay(6);

        String prompt = promptBuilder.buildUserPrompt(plan);

        assertThat(prompt).contains("Assign the long run to Saturday each week.");
    }

    @Test
    void formatPace_300seconds_returns5ColonZeroZero() {
        assertThat(promptBuilder.formatPace(300)).isEqualTo("5:00/km");
    }

    @Test
    void formatPace_270seconds_returns4Colon30() {
        assertThat(promptBuilder.formatPace(270)).isEqualTo("4:30/km");
    }

    @Test
    void formatPace_345seconds_returns5Colon45() {
        assertThat(promptBuilder.formatPace(345)).isEqualTo("5:45/km");
    }

    @Test
    void toHumanReadableDistance_allValues() {
        assertThat(promptBuilder.toHumanReadableDistance(PlanDistance.FIVE_K)).isEqualTo("5K");
        assertThat(promptBuilder.toHumanReadableDistance(PlanDistance.TEN_K)).isEqualTo("10K");
        assertThat(promptBuilder.toHumanReadableDistance(PlanDistance.HALF_MARATHON)).isEqualTo("Half Marathon");
        assertThat(promptBuilder.toHumanReadableDistance(PlanDistance.MARATHON)).isEqualTo("Marathon");
    }

    @Test
    void toDayName_allDays() {
        assertThat(promptBuilder.toDayName(1)).isEqualTo("Monday");
        assertThat(promptBuilder.toDayName(2)).isEqualTo("Tuesday");
        assertThat(promptBuilder.toDayName(3)).isEqualTo("Wednesday");
        assertThat(promptBuilder.toDayName(4)).isEqualTo("Thursday");
        assertThat(promptBuilder.toDayName(5)).isEqualTo("Friday");
        assertThat(promptBuilder.toDayName(6)).isEqualTo("Saturday");
        assertThat(promptBuilder.toDayName(7)).isEqualTo("Sunday");
    }

    private TrainingPlan createTestPlan() {
        TrainingPlan plan = new TrainingPlan();
        plan.setEventName("Spring Marathon 2025");
        plan.setDistance(PlanDistance.MARATHON);
        plan.setDuration(PlanDuration.WEEKS_12);
        plan.setRaceDate(LocalDate.of(2025, 9, 15));
        plan.setTargetPaceSecondsPerKm(300);
        plan.setAiModel(AiModel.CLAUDE);
        plan.setTrainingDays(List.of(1, 3, 5, 7));
        plan.setLongRunDay(7);
        return plan;
    }
}
