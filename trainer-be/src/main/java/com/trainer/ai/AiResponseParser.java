package com.trainer.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw AI model response strings into {@link AiPlanResponse} DTOs.
 *
 * <p>Handles common AI response quirks:</p>
 * <ul>
 *   <li>Markdown code fences ({@code ```json ... ```})</li>
 *   <li>Extra text before/after the JSON object</li>
 *   <li>Malformed JSON or schema mismatches</li>
 * </ul>
 *
 * <p>Throws {@link AiResponseParseException} if the response cannot be parsed,
 * and {@link AiResponseValidationException} if the parsed response has an empty
 * or missing {@code workouts} array.</p>
 */
@Component
public class AiResponseParser {

    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile(
            "```(?:json)?\\s*\\n?(.*?)\\n?\\s*```", Pattern.DOTALL);

    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile(
            "\\{.*}", Pattern.DOTALL);

    private final ObjectMapper objectMapper;

    public AiResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(JsonReadFeature.ALLOW_JAVA_COMMENTS.mappedFeature(), true)
                .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true)
                .configure(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature(), true);
    }

    /**
     * Parses a raw JSON response string into an {@link AiPlanResponse}.
     *
     * @param jsonResponse the raw response from the AI model
     * @return the parsed response DTO
     * @throws AiResponseParseException       if the JSON is malformed or does not match the schema
     * @throws AiResponseValidationException  if the workouts array is empty or missing
     */
    public AiPlanResponse parse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            throw new AiResponseParseException("AI response is null or empty");
        }

        String json = extractJson(jsonResponse);

        AiPlanResponse response;
        try {
            response = objectMapper.readValue(json, AiPlanResponse.class);
        } catch (JsonProcessingException e) {
            throw new AiResponseParseException(
                    "Failed to parse AI response as JSON: " + e.getOriginalMessage(), e);
        }

        if (response == null || response.workouts() == null || response.workouts().isEmpty()) {
            throw new AiResponseValidationException(
                    "At least one workout is required",
                    List.of("At least one workout is required"));
        }

        return response;
    }

    /**
     * Extracts JSON content from the raw response, handling markdown code fences
     * and extra surrounding text.
     */
    private String extractJson(String rawResponse) {
        String trimmed = rawResponse.trim();

        // Try to extract from markdown code fences first
        Matcher codeFenceMatcher = CODE_FENCE_PATTERN.matcher(trimmed);
        if (codeFenceMatcher.find()) {
            return codeFenceMatcher.group(1).trim();
        }

        // Try to extract a JSON object from surrounding text
        Matcher jsonObjectMatcher = JSON_OBJECT_PATTERN.matcher(trimmed);
        if (jsonObjectMatcher.find()) {
            return jsonObjectMatcher.group(0);
        }

        // Return as-is and let Jackson handle the error
        return trimmed;
    }
}
