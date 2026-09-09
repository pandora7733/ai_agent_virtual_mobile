package com.sihoo.ai_agent_virtual_mobile.character;

public final class CharacterTimings {
    public final float boredAfterSeconds;
    public final float sleepAfterSeconds;
    public final float sleepDurationSeconds;

    public CharacterTimings(
            float boredAfterSeconds,
            float sleepAfterSeconds,
            float sleepDurationSeconds
    ) {
        this.boredAfterSeconds = boredAfterSeconds;
        this.sleepAfterSeconds = sleepAfterSeconds;
        this.sleepDurationSeconds = sleepDurationSeconds;
    }

    public static CharacterTimings defaults() {
        return new CharacterTimings(
                5.0f * 60.0f,
                8.0f * 60.0f,
                8.0f * 60.0f
        );
    }
}
