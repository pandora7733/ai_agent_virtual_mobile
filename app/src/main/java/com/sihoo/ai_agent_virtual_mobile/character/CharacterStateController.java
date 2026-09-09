package com.sihoo.ai_agent_virtual_mobile.character;

import android.util.Log;

public class CharacterStateController {
    private static final String TOUCH_LOG_TAG = "PetTouch";
    private static final String OUTFIT_LOG_TAG = "Outfit";
    private static final String LIFECYCLE_LOG_TAG = "PetLifecycle";
    private static final String STATE_LOG_TAG = "PetState";

    private final CharacterMotionPlayer player;
    private final PetRepository repository;
    private final CharacterTimings timings;

    private CharacterState currentState = CharacterState.LOADING;
    private OutfitType currentOutfit = OutfitType.DEFAULT;
    private boolean screenWasHidden = false;
    private float inactivityElapsedSeconds = 0.0f;
    private float sleepElapsedSeconds = 0.0f;
    private boolean boredPlayedForCurrentInactivity = false;
    private String pendingWakeReason = "";

    public CharacterStateController(
            CharacterMotionPlayer player,
            PetRepository repository,
            CharacterTimings timings
    ) {
        if (player == null || repository == null || timings == null) {
            throw new IllegalArgumentException("controller dependencies are null");
        }
        this.player = player;
        this.repository = repository;
        this.timings = timings;
    }

    public void onModelReady() {
        currentOutfit = repository.getOutfit();
        playAppearanceMotion("load");
    }

    public void onScreenHidden() {
        screenWasHidden = true;
        touchLastActivity();
        Log.d(LIFECYCLE_LOG_TAG, "screen hidden state=" + currentState);
    }

    public void onScreenShown() {
        if (!screenWasHidden) {
            return;
        }

        screenWasHidden = false;
        playAppearanceMotion("visible");
    }

    public void update(float deltaTimeSeconds) {
        if (!player.hasModel()) {
            return;
        }

        float deltaTime = Math.max(0.0f, Math.min(deltaTimeSeconds, 0.1f));

        if (currentState == CharacterState.FIRST_VISIT
                && player.isAppearanceMotionFinished()) {
            finishAppearance();
        }

        updateInactivityTimer(deltaTime);
    }

    public boolean applyOutfit(OutfitType outfitType) {
        return applyOutfit(outfitType, false);
    }

    public boolean applyOutfit(OutfitType outfitType, boolean force) {
        if (!player.hasModel() || outfitType == null) {
            return false;
        }

        if (!force && outfitType == currentOutfit) {
            return true;
        }

        boolean applied = player.applyOutfitVisual(outfitType);
        if (applied) {
            currentOutfit = outfitType;
            repository.saveOutfit(outfitType);
            Log.d(OUTFIT_LOG_TAG, "apply outfit=" + outfitType);
        } else {
            Log.d(OUTFIT_LOG_TAG, "apply outfit failed=" + outfitType);
        }
        return applied;
    }

    public OutfitType getCurrentOutfit() {
        return currentOutfit;
    }

    public CharacterState getCurrentState() {
        return currentState;
    }

    public boolean onUserActivity() {
        if (currentState == CharacterState.FIRST_VISIT) {
            Log.d(TOUCH_LOG_TAG, "USER_ACTIVITY ignore gestures state=FIRST_VISIT");
            return true;
        }

        resetInteractionTimers();
        touchLastActivity();

        if (currentState == CharacterState.BORED) {
            player.stopBoredMotion();
            currentState = CharacterState.IDLE;
            logState("BORED -> IDLE (user activity)");
            Log.d(TOUCH_LOG_TAG, "USER_ACTIVITY cancel BORED");
            return true;
        } else if (currentState == CharacterState.SLEEP) {
            Log.d(TOUCH_LOG_TAG, "USER_ACTIVITY wake SLEEP");
            wakeUpFromSleep("user activity");
            return true;
        }

        return false;
    }

    public boolean canStartInteraction() {
        return currentState == CharacterState.IDLE
                || currentState == CharacterState.HEAD_PAT
                || currentState == CharacterState.HEAD_DOUBLE_TAP
                || currentState == CharacterState.BODY_STROKE
                || currentState == CharacterState.BODY_DOUBLE_TAP;
    }

    public void onHeadPat(float patX, float patY) {
        if (!player.hasModel()) {
            return;
        }

        stopBodyInteractionForHead();

        if (currentState == CharacterState.HEAD_DOUBLE_TAP) {
            player.stopHeadDoubleTapMotion();
            currentState = CharacterState.IDLE;
        }

        if (currentState == CharacterState.HEAD_PAT
                && player.isHeadPatReleasing()) {
            if (!player.startHeadPatMotion()) {
                return;
            }
        }

        if (currentState == CharacterState.IDLE) {
            if (!player.startHeadPatMotion()) {
                return;
            }

            currentState = CharacterState.HEAD_PAT;
            resetInteractionTimers();
            player.setIdleEffectsEnabled(true);
            logState("IDLE -> HEAD_PAT");
            Log.d(TOUCH_LOG_TAG, "STATE IDLE -> HEAD_PAT");
        }

        if (currentState == CharacterState.HEAD_PAT) {
            player.updateHeadPat(patX, patY);
        }
    }

    public void onHeadPatEnd() {
        if (currentState != CharacterState.HEAD_PAT) {
            Log.d(TOUCH_LOG_TAG, "HEAD_PAT_END ignored state=" + currentState);
            return;
        }

        Log.d(TOUCH_LOG_TAG, "HEAD_PAT_END release");
        player.endHeadPatMotion();
    }

    public void cancelHeadPat() {
        if (currentState != CharacterState.HEAD_PAT) {
            return;
        }

        player.stopHeadPatMotion();
        currentState = CharacterState.IDLE;
        inactivityElapsedSeconds = 0.0f;
        logState("HEAD_PAT -> IDLE (cancel)");
        Log.d(TOUCH_LOG_TAG, "STATE HEAD_PAT -> IDLE (cancel)");
    }

    public void onHeadDoubleTap() {
        if (!player.hasModel() || currentState != CharacterState.IDLE) {
            Log.d(
                    TOUCH_LOG_TAG,
                    "HEAD_DOUBLE_TAP ignored state=" + currentState
            );
            return;
        }

        if (!player.startHeadDoubleTapMotion()) {
            return;
        }

        currentState = CharacterState.HEAD_DOUBLE_TAP;
        resetInteractionTimers();
        logState("IDLE -> HEAD_DOUBLE_TAP");
        Log.d(TOUCH_LOG_TAG, "STATE IDLE -> HEAD_DOUBLE_TAP");
    }

    public void onBodyStroke(float strokeX, float strokeY, boolean chest) {
        if (!player.hasModel()) {
            return;
        }

        stopHeadInteractionForBody();

        if (currentState == CharacterState.BODY_DOUBLE_TAP) {
            interruptBodyDoubleTap("stroke");
        }

        if (currentState == CharacterState.BODY_STROKE
                && player.isBodyStrokeReleasing()) {
            if (!player.startBodyStrokeMotion(chest)) {
                return;
            }
        }

        if (currentState == CharacterState.IDLE) {
            if (!player.startBodyStrokeMotion(chest)) {
                return;
            }

            currentState = CharacterState.BODY_STROKE;
            resetInteractionTimers();
            player.setIdleEffectsEnabled(true);
            String region = chest ? "CHEST" : "BELLY";
            logState("IDLE -> BODY_STROKE region=" + region);
            Log.d(TOUCH_LOG_TAG, "STATE IDLE -> BODY_STROKE region=" + region);
        }

        if (currentState == CharacterState.BODY_STROKE) {
            player.updateBodyStroke(strokeX, strokeY);
        }
    }

    public void onBodyStrokeEnd() {
        if (currentState != CharacterState.BODY_STROKE) {
            Log.d(
                    TOUCH_LOG_TAG,
                    "BODY_STROKE_END ignored state=" + currentState
            );
            return;
        }

        Log.d(TOUCH_LOG_TAG, "BODY_STROKE_END release");
        player.endBodyStrokeMotion();
    }

    public void cancelBodyStroke() {
        if (currentState != CharacterState.BODY_STROKE) {
            return;
        }

        player.stopBodyStrokeMotion();
        currentState = CharacterState.IDLE;
        inactivityElapsedSeconds = 0.0f;
        logState("BODY_STROKE -> IDLE (cancel)");
        Log.d(TOUCH_LOG_TAG, "STATE BODY_STROKE -> IDLE (cancel)");
    }

    public void onBodyDoubleTap(boolean chest) {
        if (!player.hasModel() || currentState != CharacterState.IDLE) {
            Log.d(
                    TOUCH_LOG_TAG,
                    "BODY_DOUBLE_TAP ignored state="
                            + currentState
                            + " chest="
                            + chest
            );
            return;
        }

        if (!player.startBodyDoubleTapMotion(chest)) {
            return;
        }

        boolean surprised = player.startSurprisedExpression();
        currentState = CharacterState.BODY_DOUBLE_TAP;
        resetInteractionTimers();

        String region = chest ? "CHEST" : "BELLY";
        logState("IDLE -> BODY_DOUBLE_TAP region=" + region);
        Log.d(
                TOUCH_LOG_TAG,
                "STATE IDLE -> BODY_DOUBLE_TAP region="
                        + region
                        + " surprised="
                        + surprised
        );
    }

    public void cancelBodyDoubleTap() {
        interruptBodyDoubleTap("pinch");
    }

    private void playAppearanceMotion(String reason) {
        if (!player.hasModel()) {
            return;
        }

        player.clearFaceExpression();
        player.setIdleEffectsEnabled(false);
        if (!player.startAppearanceMotion()) {
            return;
        }
        applyOutfit(currentOutfit, true);

        currentState = CharacterState.FIRST_VISIT;
        inactivityElapsedSeconds = 0.0f;
        sleepElapsedSeconds = 0.0f;
        boredPlayedForCurrentInactivity = false;
        pendingWakeReason = "";

        logState("-> FIRST_VISIT (" + reason + ")");
        Log.d(
                LIFECYCLE_LOG_TAG,
                "appearance start reason="
                        + reason
                        + " outfit="
                        + currentOutfit
        );
    }

    private void finishAppearance() {
        if (repository.isFirstVisit()) {
            repository.markFirstVisitCompleted();
        }

        currentState = CharacterState.IDLE;
        player.setIdleEffectsEnabled(true);
        applyOutfit(currentOutfit, true);
        resetInteractionTimers();
        sleepElapsedSeconds = 0.0f;
        touchLastActivity();

        logState("FIRST_VISIT -> IDLE");
        Log.d(LIFECYCLE_LOG_TAG, "appearance finished outfit=" + currentOutfit);
    }

    private void updateInactivityTimer(float deltaTime) {
        switch (currentState) {
            case IDLE:
                inactivityElapsedSeconds += deltaTime;

                if (inactivityElapsedSeconds >= timings.sleepAfterSeconds) {
                    enterSleep("IDLE");
                } else if (!boredPlayedForCurrentInactivity
                        && inactivityElapsedSeconds >= timings.boredAfterSeconds) {
                    if (player.startBoredMotion()) {
                        boredPlayedForCurrentInactivity = true;
                        currentState = CharacterState.BORED;
                        logState("IDLE -> BORED");
                    }
                }
                break;

            case BORED:
                inactivityElapsedSeconds += deltaTime;

                if (player.isBoredMotionFinished()) {
                    if (inactivityElapsedSeconds >= timings.sleepAfterSeconds) {
                        enterSleep("BORED");
                    } else {
                        currentState = CharacterState.IDLE;
                        logState("BORED -> IDLE");
                    }
                }
                break;

            case HEAD_PAT:
            case HEAD_DOUBLE_TAP:
            case BODY_STROKE:
            case BODY_DOUBLE_TAP:
                if (currentState == CharacterState.HEAD_PAT
                        && player.isHeadPatFinished()) {
                    currentState = CharacterState.IDLE;
                    inactivityElapsedSeconds = 0.0f;
                    logState("HEAD_PAT -> IDLE");
                } else if (currentState == CharacterState.HEAD_DOUBLE_TAP
                        && player.isHeadDoubleTapFinished()) {
                    currentState = CharacterState.IDLE;
                    inactivityElapsedSeconds = 0.0f;
                    logState("HEAD_DOUBLE_TAP -> IDLE");
                } else if (currentState == CharacterState.BODY_STROKE
                        && player.isBodyStrokeFinished()) {
                    currentState = CharacterState.IDLE;
                    inactivityElapsedSeconds = 0.0f;
                    logState("BODY_STROKE -> IDLE");
                } else if (currentState == CharacterState.BODY_DOUBLE_TAP
                        && player.isBodyDoubleTapFinished()) {
                    finishBodyDoubleTap("finished");
                    logState("BODY_DOUBLE_TAP -> IDLE");
                }
                break;

            case SLEEP_ENTRY:
                if (player.isSleepEntryFinished()) {
                    if (player.startSleepLoopMotion()) {
                        currentState = CharacterState.SLEEP;
                        sleepElapsedSeconds = 0.0f;
                        logState("SLEEP_ENTRY -> SLEEP");
                    }
                }
                break;

            case SLEEP:
                sleepElapsedSeconds += deltaTime;
                if (sleepElapsedSeconds >= timings.sleepDurationSeconds) {
                    wakeUpFromSleep("timeout");
                }
                break;

            case WAKING:
                if (player.isWakeMotionFinished()) {
                    finishWake();
                }
                break;

            default:
                break;
        }
    }

    private void enterSleep(String previousStateName) {
        if (!player.startSleepEntryMotion()) {
            return;
        }

        currentState = CharacterState.SLEEP_ENTRY;
        player.setIdleEffectsEnabled(false);
        logState(previousStateName + " -> SLEEP_ENTRY");
    }

    private void wakeUpFromSleep(String reason) {
        if (!player.startWakeMotion()) {
            return;
        }

        pendingWakeReason = reason;
        currentState = CharacterState.WAKING;
        logState("SLEEP -> WAKING (" + reason + ")");
    }

    private void finishWake() {
        player.finishWakeMotion();
        player.setIdleEffectsEnabled(true);
        currentState = CharacterState.IDLE;
        resetInteractionTimers();
        sleepElapsedSeconds = 0.0f;
        logState("WAKING -> IDLE (" + pendingWakeReason + ")");
        pendingWakeReason = "";
    }

    private void stopBodyInteractionForHead() {
        if (currentState == CharacterState.BODY_STROKE) {
            Log.d(TOUCH_LOG_TAG, "INTERRUPT BODY_STROKE by HEAD");
            player.stopBodyStrokeMotion();
            currentState = CharacterState.IDLE;
        } else if (currentState == CharacterState.BODY_DOUBLE_TAP) {
            Log.d(TOUCH_LOG_TAG, "INTERRUPT BODY_DOUBLE_TAP by HEAD");
            interruptBodyDoubleTap("head");
        }
    }

    private void stopHeadInteractionForBody() {
        if (currentState == CharacterState.HEAD_PAT) {
            Log.d(TOUCH_LOG_TAG, "INTERRUPT HEAD_PAT by BODY");
            player.stopHeadPatMotion();
            currentState = CharacterState.IDLE;
        } else if (currentState == CharacterState.HEAD_DOUBLE_TAP) {
            Log.d(TOUCH_LOG_TAG, "INTERRUPT HEAD_DOUBLE_TAP by BODY");
            player.stopHeadDoubleTapMotion();
            currentState = CharacterState.IDLE;
        }
    }

    private void interruptBodyDoubleTap(String reason) {
        if (currentState != CharacterState.BODY_DOUBLE_TAP) {
            return;
        }

        player.stopBodyDoubleTapMotion();
        finishBodyDoubleTap(reason);
    }

    private void finishBodyDoubleTap(String reason) {
        currentState = CharacterState.IDLE;
        inactivityElapsedSeconds = 0.0f;
        player.clearFaceExpression();
        Log.d(
                TOUCH_LOG_TAG,
                "BODY_DOUBLE_TAP end reason="
                        + reason
                        + " outfit="
                        + currentOutfit
        );
    }

    private void resetInteractionTimers() {
        inactivityElapsedSeconds = 0.0f;
        boredPlayedForCurrentInactivity = false;
    }

    private void touchLastActivity() {
        repository.saveLastActivityAt(System.currentTimeMillis());
    }

    private static void logState(String message) {
        Log.d(STATE_LOG_TAG, "[APP] state changed: " + message);
    }
}
