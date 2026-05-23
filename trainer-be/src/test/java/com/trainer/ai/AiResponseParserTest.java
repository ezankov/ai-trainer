package com.trainer.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiResponseParserTest {

    private AiResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new AiResponseParser(new ObjectMapper());
    }

    @Test
    void parse_validJson_returnsAiPlanResponse() {
        String json = """
                {
                  "workouts": [
                    {
                      "name": "Easy Run",
                      "sportType": "RUNNING",
                      "subSport": null,
                      "steps": [
                        {
                          "stepOrder": 1,
                          "stepName": "Warm Up",
                          "intensity": "WARMUP",
                          "durationType": "TIME",
                          "durationValue": 600,
                          "targetType": "OPEN",
                          "targetValueLow": null,
                          "targetValueHigh": null
                        }
                      ],
                      "schedule": {
                        "weekNumber": 1,
                        "dayOfWeek": 1,
                        "orderInDay": 1
                      }
                    }
                  ]
                }
                """;

        AiPlanResponse response = parser.parse(json);

        assertThat(response.workouts()).hasSize(1);
        assertThat(response.workouts().get(0).name()).isEqualTo("Easy Run");
        assertThat(response.workouts().get(0).steps()).hasSize(1);
        assertThat(response.workouts().get(0).steps().get(0).intensity()).isEqualTo("WARMUP");
        assertThat(response.workouts().get(0).schedule().weekNumber()).isEqualTo(1);
    }

    @Test
    void parse_jsonWrappedInCodeFences_extractsAndParses() {
        String response = """
                Here is your training plan:
                ```json
                {
                  "workouts": [
                    {
                      "name": "Tempo Run",
                      "sportType": "RUNNING",
                      "subSport": null,
                      "steps": [
                        {
                          "stepOrder": 1,
                          "stepName": "Main Set",
                          "intensity": "ACTIVE",
                          "durationType": "TIME",
                          "durationValue": 1200,
                          "targetType": "SPEED",
                          "targetValueLow": 240,
                          "targetValueHigh": 260
                        }
                      ],
                      "schedule": {
                        "weekNumber": 2,
                        "dayOfWeek": 3,
                        "orderInDay": 1
                      }
                    }
                  ]
                }
                ```
                """;

        AiPlanResponse result = parser.parse(response);

        assertThat(result.workouts()).hasSize(1);
        assertThat(result.workouts().get(0).name()).isEqualTo("Tempo Run");
    }

    @Test
    void parse_jsonWithSurroundingText_extractsAndParses() {
        String response = """
                I've created a training plan for you:
                {"workouts":[{"name":"Long Run","sportType":"RUNNING","subSport":null,"steps":[{"stepOrder":1,"stepName":"Easy","intensity":"ACTIVE","durationType":"TIME","durationValue":3600,"targetType":"OPEN","targetValueLow":null,"targetValueHigh":null}],"schedule":{"weekNumber":1,"dayOfWeek":7,"orderInDay":1}}]}
                Hope this helps!
                """;

        AiPlanResponse result = parser.parse(response);

        assertThat(result.workouts()).hasSize(1);
        assertThat(result.workouts().get(0).name()).isEqualTo("Long Run");
    }

    @Test
    void parse_malformedJson_throwsAiResponseParseException() {
        String malformed = "{ this is not valid json }";

        assertThatThrownBy(() -> parser.parse(malformed))
                .isInstanceOf(AiResponseParseException.class)
                .hasMessageContaining("Failed to parse AI response as JSON");
    }

    @Test
    void parse_nullInput_throwsAiResponseParseException() {
        assertThatThrownBy(() -> parser.parse(null))
                .isInstanceOf(AiResponseParseException.class)
                .hasMessageContaining("AI response is null or empty");
    }

    @Test
    void parse_blankInput_throwsAiResponseParseException() {
        assertThatThrownBy(() -> parser.parse("   "))
                .isInstanceOf(AiResponseParseException.class)
                .hasMessageContaining("AI response is null or empty");
    }

    @Test
    void parse_emptyWorkoutsArray_throwsAiResponseValidationException() {
        String json = """
                { "workouts": [] }
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AiResponseValidationException.class)
                .hasMessageContaining("At least one workout is required");
    }

    @Test
    void parse_missingWorkoutsField_throwsAiResponseValidationException() {
        String json = """
                { "someOtherField": "value" }
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AiResponseValidationException.class)
                .hasMessageContaining("At least one workout is required");
    }

    @Test
    void parse_nullWorkoutsField_throwsAiResponseValidationException() {
        String json = """
                { "workouts": null }
                """;

        assertThatThrownBy(() -> parser.parse(json))
                .isInstanceOf(AiResponseValidationException.class)
                .hasMessageContaining("At least one workout is required");
    }

    @Test
    void parse_codeFencesWithoutJsonLabel_extractsAndParses() {
        String response = """
                ```
                {
                  "workouts": [
                    {
                      "name": "Recovery Run",
                      "sportType": "RUNNING",
                      "subSport": null,
                      "steps": [
                        {
                          "stepOrder": 1,
                          "stepName": "Easy Jog",
                          "intensity": "RECOVERY",
                          "durationType": "TIME",
                          "durationValue": 1800,
                          "targetType": "HEART_RATE",
                          "targetValueLow": 120,
                          "targetValueHigh": 140
                        }
                      ],
                      "schedule": {
                        "weekNumber": 1,
                        "dayOfWeek": 2,
                        "orderInDay": 1
                      }
                    }
                  ]
                }
                ```
                """;

        AiPlanResponse result = parser.parse(response);

        assertThat(result.workouts()).hasSize(1);
        assertThat(result.workouts().get(0).name()).isEqualTo("Recovery Run");
    }

    @Test
    void parse_completelyInvalidString_throwsAiResponseParseException() {
        String invalid = "I cannot generate a training plan for you.";

        assertThatThrownBy(() -> parser.parse(invalid))
                .isInstanceOf(AiResponseParseException.class);
    }

    @Test
    void parse_unknownFieldsAreIgnored() {
        String json = """
                {
                  "workouts": [
                    {
                      "name": "Easy Run",
                      "sportType": "RUNNING",
                      "subSport": null,
                      "extraField": "ignored",
                      "steps": [
                        {
                          "stepOrder": 1,
                          "stepName": "Warm Up",
                          "intensity": "WARMUP",
                          "durationType": "TIME",
                          "durationValue": 600,
                          "targetType": "OPEN",
                          "targetValueLow": null,
                          "targetValueHigh": null,
                          "unknownField": 42
                        }
                      ],
                      "schedule": {
                        "weekNumber": 1,
                        "dayOfWeek": 1,
                        "orderInDay": 1
                      }
                    }
                  ]
                }
                """;

        AiPlanResponse result = parser.parse(json);

        assertThat(result.workouts()).hasSize(1);
        assertThat(result.workouts().get(0).name()).isEqualTo("Easy Run");
    }
}
