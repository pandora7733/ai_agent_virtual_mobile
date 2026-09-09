package com.sihoo.ai_agent_virtual_mobile.character;

final class FakeCharacterMotionPlayer implements CharacterMotionPlayer {
    boolean hasModel = true;
    boolean appearanceFinished = false;
    boolean boredFinished = false;
    boolean sleepEntryFinished = false;
    boolean wakeFinished = false;
    boolean headPatFinished = false;
    boolean headPatReleasing = false;
    boolean headDoubleTapFinished = false;
    boolean bodyStrokeFinished = false;
    boolean bodyStrokeReleasing = false;
    boolean bodyDoubleTapFinished = false;

    boolean idleEffectsEnabled = true;
    OutfitType appliedOutfit;
    boolean faceCleared;
    boolean surprisedStarted;
    String lastStarted = "";

    @Override
    public boolean hasModel() {
        return hasModel;
    }

    @Override
    public void setIdleEffectsEnabled(boolean enabled) {
        idleEffectsEnabled = enabled;
    }

    @Override
    public void clearFaceExpression() {
        faceCleared = true;
        surprisedStarted = false;
    }

    @Override
    public boolean applyOutfitVisual(OutfitType outfitType) {
        appliedOutfit = outfitType;
        return true;
    }

    @Override
    public boolean startAppearanceMotion() {
        lastStarted = "appearance";
        appearanceFinished = false;
        return hasModel;
    }

    @Override
    public boolean isAppearanceMotionFinished() {
        return appearanceFinished;
    }

    @Override
    public boolean startBoredMotion() {
        lastStarted = "bored";
        boredFinished = false;
        return true;
    }

    @Override
    public void stopBoredMotion() {
        boredFinished = true;
    }

    @Override
    public boolean isBoredMotionFinished() {
        return boredFinished;
    }

    @Override
    public boolean startSleepEntryMotion() {
        lastStarted = "sleepEntry";
        sleepEntryFinished = false;
        return true;
    }

    @Override
    public boolean isSleepEntryFinished() {
        return sleepEntryFinished;
    }

    @Override
    public boolean startSleepLoopMotion() {
        lastStarted = "sleepLoop";
        return true;
    }

    @Override
    public boolean startWakeMotion() {
        lastStarted = "wake";
        wakeFinished = false;
        return true;
    }

    @Override
    public boolean isWakeMotionFinished() {
        return wakeFinished;
    }

    @Override
    public void finishWakeMotion() {
        wakeFinished = true;
    }

    @Override
    public boolean startHeadPatMotion() {
        lastStarted = "headPat";
        headPatFinished = false;
        return true;
    }

    @Override
    public void updateHeadPat(float patX, float patY) {
    }

    @Override
    public void endHeadPatMotion() {
        headPatFinished = true;
    }

    @Override
    public void stopHeadPatMotion() {
        headPatFinished = true;
    }

    @Override
    public boolean isHeadPatFinished() {
        return headPatFinished;
    }

    @Override
    public boolean isHeadPatReleasing() {
        return headPatReleasing;
    }

    @Override
    public boolean startHeadDoubleTapMotion() {
        lastStarted = "headDoubleTap";
        headDoubleTapFinished = false;
        return true;
    }

    @Override
    public void stopHeadDoubleTapMotion() {
        headDoubleTapFinished = true;
    }

    @Override
    public boolean isHeadDoubleTapFinished() {
        return headDoubleTapFinished;
    }

    @Override
    public boolean startBodyStrokeMotion(boolean chest) {
        lastStarted = chest ? "bodyStrokeChest" : "bodyStrokeBelly";
        bodyStrokeFinished = false;
        return true;
    }

    @Override
    public void updateBodyStroke(float strokeX, float strokeY) {
    }

    @Override
    public void endBodyStrokeMotion() {
        bodyStrokeFinished = true;
    }

    @Override
    public void stopBodyStrokeMotion() {
        bodyStrokeFinished = true;
    }

    @Override
    public boolean isBodyStrokeFinished() {
        return bodyStrokeFinished;
    }

    @Override
    public boolean isBodyStrokeReleasing() {
        return bodyStrokeReleasing;
    }

    @Override
    public boolean startBodyDoubleTapMotion(boolean chest) {
        lastStarted = chest ? "bodyDoubleTapChest" : "bodyDoubleTapBelly";
        bodyDoubleTapFinished = false;
        return true;
    }

    @Override
    public void stopBodyDoubleTapMotion() {
        bodyDoubleTapFinished = true;
    }

    @Override
    public boolean isBodyDoubleTapFinished() {
        return bodyDoubleTapFinished;
    }

    @Override
    public boolean startSurprisedExpression() {
        surprisedStarted = true;
        return true;
    }
}
