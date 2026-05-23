package com.trainer.ai;

import com.trainer.trainingplan.AiModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

/**
 * Configuration class that creates conditional {@link SpringAiPlanGenerator} beans
 * for each supported AI model. Each bean is only created when the corresponding
 * model is enabled via configuration properties.
 *
 * <p>Also provides a {@code Map<AiModel, SpringAiPlanGenerator>} bean that the
 * {@link AiPlanGeneratorFactory} uses to resolve generators by model.</p>
 */
@Configuration
@EnableConfigurationProperties(AiModelProperties.class)
public class AiConfiguration {

    @Bean
    @ConditionalOnProperty(name = "trainer.ai.chatgpt.enabled", havingValue = "true")
    public SpringAiPlanGenerator chatgptPlanGenerator(
            ChatModel chatModel,
            AiPromptBuilder promptBuilder,
            AthleteProfileTool athleteProfileTool,
            AiResponseParser responseParser,
            AiResponseValidator responseValidator,
            AiResponseMapper responseMapper) {
        return new SpringAiPlanGenerator(chatModel, promptBuilder, athleteProfileTool,
                responseParser, responseValidator, responseMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "trainer.ai.claude.enabled", havingValue = "true")
    public SpringAiPlanGenerator claudePlanGenerator(
            ChatModel chatModel,
            AiPromptBuilder promptBuilder,
            AthleteProfileTool athleteProfileTool,
            AiResponseParser responseParser,
            AiResponseValidator responseValidator,
            AiResponseMapper responseMapper) {
        return new SpringAiPlanGenerator(chatModel, promptBuilder, athleteProfileTool,
                responseParser, responseValidator, responseMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "trainer.ai.gemini.enabled", havingValue = "true")
    public SpringAiPlanGenerator geminiPlanGenerator(
            ChatModel chatModel,
            AiPromptBuilder promptBuilder,
            AthleteProfileTool athleteProfileTool,
            AiResponseParser responseParser,
            AiResponseValidator responseValidator,
            AiResponseMapper responseMapper) {
        return new SpringAiPlanGenerator(chatModel, promptBuilder, athleteProfileTool,
                responseParser, responseValidator, responseMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "trainer.ai.kiro.enabled", havingValue = "true")
    public SpringAiPlanGenerator kiroPlanGenerator(
            ChatModel chatModel,
            AiPromptBuilder promptBuilder,
            AthleteProfileTool athleteProfileTool,
            AiResponseParser responseParser,
            AiResponseValidator responseValidator,
            AiResponseMapper responseMapper) {
        return new SpringAiPlanGenerator(chatModel, promptBuilder, athleteProfileTool,
                responseParser, responseValidator, responseMapper);
    }

    @Bean
    public Map<AiModel, SpringAiPlanGenerator> aiPlanGeneratorMap(ApplicationContext context) {
        Map<AiModel, SpringAiPlanGenerator> map = new EnumMap<>(AiModel.class);
        addIfPresent(context, map, "chatgptPlanGenerator", AiModel.CHATGPT);
        addIfPresent(context, map, "claudePlanGenerator", AiModel.CLAUDE);
        addIfPresent(context, map, "geminiPlanGenerator", AiModel.GEMINI);
        addIfPresent(context, map, "kiroPlanGenerator", AiModel.KIRO);
        return map;
    }

    private void addIfPresent(ApplicationContext context,
                              Map<AiModel, SpringAiPlanGenerator> map,
                              String beanName,
                              AiModel model) {
        if (context.containsBean(beanName)) {
            Object bean = context.getBean(beanName);
            if (bean instanceof SpringAiPlanGenerator generator) {
                map.put(model, generator);
            }
        }
    }
}
