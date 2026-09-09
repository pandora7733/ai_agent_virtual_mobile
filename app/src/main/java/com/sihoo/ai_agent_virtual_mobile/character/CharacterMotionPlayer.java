package com.sihoo.ai_agent_virtual_mobile.character;

public interface CharacterMotionPlayer {
    boolean hasModel();

    void setIdleEffectsEnabled(boolean enabled);

    void clearFaceExpression();

    boolean applyOutfitVisual(OutfitType outfitType);

    boolean startAppearanceMotion();

    boolean isAppearanceMotionFinished();

    boolean startBoredMotion();

    void stopBoredMotion();

    boolean isBoredMotionFinished();

    boolean startSleepEntryMotion();

    boolean isSleepEntryFinished();

    boolean startSleepLoopMotion();

    boolean startWakeMotion();

    boolean isWakeMotionFinished();

    void finishWakeMotion();

    boolean startHeadPatMotion();

    void updateHeadPat(float patX, float patY);

    void endHeadPatMotion();

    void stopHeadPatMotion();

    boolean isHeadPatFinished();

    boolean isHeadPatReleasing();

    boolean startHeadDoubleTapMotion();

    void stopHeadDoubleTapMotion();

    boolean isHeadDoubleTapFinished();

    boolean startBodyStrokeMotion(boolean chest);

    void updateBodyStroke(float strokeX, float strokeY);

    void endBodyStrokeMotion();

    void stopBodyStrokeMotion();

    boolean isBodyStrokeFinished();

    boolean isBodyStrokeReleasing();

    boolean startBodyDoubleTapMotion(boolean chest);

    void stopBodyDoubleTapMotion();

    boolean isBodyDoubleTapFinished();

    boolean startSurprisedExpression();
}
