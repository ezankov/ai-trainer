package com.trainer.trainingplan;

public enum PlanDuration {
    WEEKS_8, WEEKS_10, WEEKS_12;

    public int getWeeks() {
        return switch (this) {
            case WEEKS_8 -> 8;
            case WEEKS_10 -> 10;
            case WEEKS_12 -> 12;
        };
    }
}
