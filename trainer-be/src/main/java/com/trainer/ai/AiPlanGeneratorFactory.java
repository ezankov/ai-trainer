package com.trainer.ai;

import com.trainer.trainingplan.AiModel;
import com.trainer.trainingplan.DummyPlanGenerator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory that resolves the correct {@link AiPlanGenerator} implementation
 * based on the requested {@link AiModel}.
 *
 * <p>For {@code DUMMY}, returns the {@link DummyPlanGenerator} directly without
 * any configuration check. For all other models, verifies the model is available
 * (enabled and API key configured) before returning the corresponding
 * {@link SpringAiPlanGenerator} bean.</p>
 */
@Component
public class AiPlanGeneratorFactory {

    private final Map<AiModel, SpringAiPlanGenerator> generators;
    private final DummyPlanGenerator dummyPlanGenerator;
    private final AiModelProperties aiModelProperties;

    public AiPlanGeneratorFactory(Map<AiModel, SpringAiPlanGenerator> generators,
                                  DummyPlanGenerator dummyPlanGenerator,
                                  AiModelProperties aiModelProperties) {
        this.generators = generators;
        this.dummyPlanGenerator = dummyPlanGenerator;
        this.aiModelProperties = aiModelProperties;
    }

    /**
     * Resolves the correct {@link AiPlanGenerator} for the given model.
     *
     * @param aiModel the AI model to resolve a generator for
     * @return the appropriate {@link AiPlanGenerator} implementation
     * @throws AiModelNotAvailableException if the model is disabled or its API key is not configured
     * @throws AiModelNotSupportedException if no implementation is registered for the model
     */
    public AiPlanGenerator getGenerator(AiModel aiModel) {
        if (aiModel == AiModel.DUMMY) {
            return dummyPlanGenerator;
        }

        AiModelProperties.ModelConfig config = getModelConfig(aiModel);
        if (!config.isAvailable()) {
            throw new AiModelNotAvailableException(
                    "The requested AI model (" + aiModel + ") is not currently available");
        }

        SpringAiPlanGenerator generator = generators.get(aiModel);
        if (generator == null) {
            throw new AiModelNotSupportedException("AI model " + aiModel + " is not supported");
        }

        return generator;
    }

    private AiModelProperties.ModelConfig getModelConfig(AiModel aiModel) {
        return switch (aiModel) {
            case CHATGPT -> aiModelProperties.chatgpt();
            case CLAUDE -> aiModelProperties.claude();
            case GEMINI -> aiModelProperties.gemini();
            case KIRO -> aiModelProperties.kiro();
            case DUMMY -> throw new IllegalStateException("DUMMY should not reach config check");
        };
    }
}
