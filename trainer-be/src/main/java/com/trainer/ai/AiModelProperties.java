package com.trainer.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for AI model integrations.
 * Each model (chatgpt, claude, gemini, kiro) has its own nested configuration
 * with enabled flag, API key, and model identifier.
 */
@ConfigurationProperties(prefix = "trainer.ai")
public record AiModelProperties(
    ModelConfig chatgpt,
    ModelConfig claude,
    ModelConfig gemini,
    ModelConfig kiro
) {

    /**
     * Configuration for a single AI model provider.
     *
     * @param enabled whether this model is enabled (defaults to false)
     * @param apiKey  the API key for authenticating with the provider
     * @param model   the model identifier (e.g., "gpt-4o", "claude-sonnet-4-20250514")
     */
    public record ModelConfig(
        boolean enabled,
        String apiKey,
        String model
    ) {
        /**
         * Returns true only if the model is enabled AND the API key is
         * non-null and non-blank (after trimming).
         */
        public boolean isAvailable() {
            return enabled && apiKey != null && !apiKey.isBlank();
        }
    }
}
