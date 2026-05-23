package com.trainer.ai;

import com.trainer.trainingplan.TrainingPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.net.SocketTimeoutException;
import java.util.Map;

/**
 * Spring AI-backed implementation of {@link AiPlanGenerator}.
 *
 * <p>Uses Spring AI's {@link ChatClient} to communicate with an AI model,
 * registering the {@link AthleteProfileTool} as a callable tool scoped to
 * the current user via {@code ToolContext}.</p>
 *
 * <p>Instances are created by {@code AiConfiguration} as Spring beans — one
 * per enabled AI model. This class is NOT annotated with {@code @Component}.</p>
 */
public class SpringAiPlanGenerator implements AiPlanGenerator {

    private static final Logger log = LoggerFactory.getLogger(SpringAiPlanGenerator.class);

    private final ChatModel chatModel;
    private final AiPromptBuilder promptBuilder;
    private final AthleteProfileTool athleteProfileTool;
    private final AiResponseParser responseParser;
    private final AiResponseValidator responseValidator;
    private final AiResponseMapper responseMapper;

    public SpringAiPlanGenerator(ChatModel chatModel,
                                 AiPromptBuilder promptBuilder,
                                 AthleteProfileTool athleteProfileTool,
                                 AiResponseParser responseParser,
                                 AiResponseValidator responseValidator,
                                 AiResponseMapper responseMapper) {
        this.chatModel = chatModel;
        this.promptBuilder = promptBuilder;
        this.athleteProfileTool = athleteProfileTool;
        this.responseParser = responseParser;
        this.responseValidator = responseValidator;
        this.responseMapper = responseMapper;
    }

    @Override
    public void generate(TrainingPlan plan) {
        log.info("Starting AI plan generation for plan {} with model {}", plan.getId(), chatModel.getClass().getSimpleName());
        String responseContent;
        try {
            ChatClient client = ChatClient.create(chatModel);

            log.debug("System prompt: {}", promptBuilder.buildSystemPrompt());
            log.debug("User prompt: {}", promptBuilder.buildUserPrompt(plan));
            log.info("Calling AI model with tool context userId={}", plan.getUserId());

            responseContent = client.prompt()
                    .system(promptBuilder.buildSystemPrompt())
                    .user(promptBuilder.buildUserPrompt(plan))
                    .tools(athleteProfileTool)
                    .toolContext(Map.of("userId", plan.getUserId()))
                    .call()
                    .content();

            log.info("AI model responded successfully, response length: {}", 
                    responseContent != null ? responseContent.length() : 0);
            log.debug("AI model response: {}", responseContent);
        } catch (Exception e) {
            if (isTimeoutException(e)) {
                log.error("AI model timed out during plan generation for plan {}", plan.getId(), e);
                throw new AiGenerationTimeoutException(
                        "AI model did not respond within the configured timeout", e);
            }
            if (e instanceof AiResponseParseException || e instanceof AiResponseValidationException) {
                throw e;
            }
            log.error("AI model failed during plan generation for plan {}", plan.getId(), e);
            throw new AiGenerationException("AI model failed to generate the plan", e);
        }

        log.info("Parsing AI response for plan {}", plan.getId());
        AiPlanResponse parsed = responseParser.parse(responseContent);
        log.info("Validating AI response: {} workouts", parsed.workouts() != null ? parsed.workouts().size() : 0);
        responseValidator.validate(parsed, plan);
        log.info("Mapping and persisting AI-generated plan {}", plan.getId());
        responseMapper.mapAndPersist(parsed, plan);
        log.info("AI plan generation completed successfully for plan {}", plan.getId());
    }

    private boolean isTimeoutException(Throwable e) {
        if (e instanceof SocketTimeoutException) {
            return true;
        }
        if (e instanceof org.springframework.web.client.ResourceAccessException) {
            return true;
        }
        // Check the cause chain for timeout-related exceptions
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
            if (cause instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            if (cause instanceof org.springframework.web.client.ResourceAccessException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
