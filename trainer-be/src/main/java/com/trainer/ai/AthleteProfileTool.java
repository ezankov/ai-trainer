package com.trainer.ai;

import com.trainer.profile.AthleteProfile;
import com.trainer.profile.AthleteProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Spring AI tool that exposes the athlete's fitness profile to the AI model
 * during plan generation.
 *
 * <p>The tool takes no model-visible parameters — the user ID is injected via
 * {@link ToolContext} by the application, ensuring the AI model cannot request
 * another user's profile.</p>
 */
@Component
public class AthleteProfileTool {

    private static final Logger log = LoggerFactory.getLogger(AthleteProfileTool.class);

    private final AthleteProfileRepository athleteProfileRepository;

    public AthleteProfileTool(AthleteProfileRepository athleteProfileRepository) {
        this.athleteProfileRepository = athleteProfileRepository;
    }

    @Tool(description = "Retrieve the athlete's fitness profile including heart rate zones, " +
            "threshold pace, race times, and biometric data. Call this before generating the plan.")
    public AthleteProfileToolResponse getAthleteProfile(ToolContext toolContext) {
        Long userId = (Long) toolContext.getContext().get("userId");
        log.info("AthleteProfileTool called for userId={}", userId);
        AthleteProfile profile = athleteProfileRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("No athlete profile found for userId={}", userId);
                    return new AthleteProfileNotFoundException(
                            "No athlete profile exists for this user");
                });
        log.info("AthleteProfileTool returning profile for userId={}", userId);
        return mapToResponse(profile);
    }

    private AthleteProfileToolResponse mapToResponse(AthleteProfile profile) {
        return new AthleteProfileToolResponse(
                profile.getDateOfBirth(),
                profile.getWeightKg(),
                profile.getRestingHR(),
                profile.getMaxHR(),
                profile.getLthr(),
                profile.getThresholdPaceSecondsPerKm(),
                profile.getVo2Max(),
                profile.getFiveKSeconds(),
                profile.getTenKSeconds(),
                profile.getHalfMarathonSeconds(),
                profile.getMarathonSeconds()
        );
    }
}
