package com.trainer.auth;

import com.trainer.ai.AiGenerationException;
import com.trainer.ai.AiGenerationTimeoutException;
import com.trainer.ai.AiModelNotAvailableException;
import com.trainer.ai.AiModelNotSupportedException;
import com.trainer.ai.AiResponseParseException;
import com.trainer.ai.AiResponseValidationException;
import com.trainer.ai.AthleteProfileNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerAiTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void aiModelNotAvailableException_returns400WithExceptionMessage() throws Exception {
        mockMvc.perform(get("/test/ai-exception").param("type", "model-not-available"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The requested AI model (CLAUDE) is not currently available"));
    }

    @Test
    void aiModelNotSupportedException_returns400WithExceptionMessage() throws Exception {
        mockMvc.perform(get("/test/ai-exception").param("type", "model-not-supported"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("AI model XYZ is not supported"));
    }

    @Test
    void athleteProfileNotFoundException_returns400WithExceptionMessage() throws Exception {
        mockMvc.perform(get("/test/ai-exception").param("type", "profile-not-found"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("An athlete profile is required for AI-generated plans"));
    }

    @Test
    void aiGenerationException_returns502WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test/ai-exception").param("type", "generation"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("AI model failed to generate the plan"));
    }

    @Test
    void aiGenerationException_doesNotExposeInternalDetails() throws Exception {
        mockMvc.perform(get("/test/ai-exception").param("type", "generation"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("AI model failed to generate the plan"))
                .andExpect(jsonPath("$.field").doesNotExist());
    }

    @Test
    void aiGenerationTimeoutException_returns504WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test/ai-exception").param("type", "timeout"))
                .andExpect(status().is(HttpStatus.GATEWAY_TIMEOUT.value()))
                .andExpect(jsonPath("$.message").value("AI model timed out while generating the plan"));
    }

    @Test
    void aiResponseValidationException_returns502WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test/ai-exception").param("type", "validation"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("AI model returned an invalid response"));
    }

    @Test
    void aiResponseValidationException_doesNotExposeValidationDetails() throws Exception {
        mockMvc.perform(get("/test/ai-exception").param("type", "validation"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("AI model returned an invalid response"))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.field").doesNotExist());
    }

    @Test
    void aiResponseParseException_returns502WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test/ai-exception").param("type", "parse"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("AI model returned an invalid response"));
    }

    @Test
    void aiResponseParseException_doesNotExposeRawResponse() throws Exception {
        mockMvc.perform(get("/test/ai-exception").param("type", "parse"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("AI model returned an invalid response"))
                .andExpect(jsonPath("$.field").doesNotExist());
    }

    /**
     * Test controller that throws AI exceptions based on a query parameter.
     */
    @RestController
    static class TestController {

        @GetMapping("/test/ai-exception")
        public String throwException(@RequestParam String type) {
            switch (type) {
                case "model-not-available" ->
                        throw new AiModelNotAvailableException("The requested AI model (CLAUDE) is not currently available");
                case "model-not-supported" ->
                        throw new AiModelNotSupportedException("AI model XYZ is not supported");
                case "profile-not-found" ->
                        throw new AthleteProfileNotFoundException("An athlete profile is required for AI-generated plans");
                case "generation" ->
                        throw new AiGenerationException("OpenAI API returned 500: Internal Server Error", 500);
                case "timeout" ->
                        throw new AiGenerationTimeoutException("AI model did not respond within 60 seconds");
                case "validation" ->
                        throw new AiResponseValidationException(
                                "AI response validation failed",
                                List.of("weekNumber 15 exceeds plan duration of 12 weeks", "invalid intensity: SPRINT"));
                case "parse" ->
                        throw new AiResponseParseException("Failed to parse AI response: unexpected token at position 42");
                default -> throw new IllegalArgumentException("Unknown type: " + type);
            }
        }
    }
}
