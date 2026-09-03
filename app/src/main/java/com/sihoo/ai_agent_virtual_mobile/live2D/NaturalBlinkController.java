package com.sihoo.ai_agent_virtual_mobile.live2D;

import com.live2d.sdk.cubism.framework.id.CubismId;
import com.live2d.sdk.cubism.framework.model.CubismModel;

public class NaturalBlinkController {
    private static final float MIN_BLINK_INTERVAL = 4.0f;
    private static final float MAX_BLINK_INTERVAL = 10.0f;

    private static final float CLOSING_DURATION = 0.12f;
    private static final float CLOSED_DURATION = 0.06f;
    private static final float OPENING_DURATION = 0.16f;

    private enum BlinkState {
        WAITING,
        CLOSING,
        CLOSED,
        OPENING
    }

    private final CubismId idParamEyeLOpen;
    private final CubismId idParamEyeROpen;

    private BlinkState state = BlinkState.WAITING;
    private float stateElapsed = 0.0f;
    private float nextBlinkTime = randomBlinkInterval();

    public NaturalBlinkController(
            CubismId idParamEyeLOpen,
            CubismId idParamEyeROpen
    ) {
        this.idParamEyeLOpen = idParamEyeLOpen;
        this.idParamEyeROpen = idParamEyeROpen;
    }

    public void reset() {
        state = BlinkState.WAITING;
        stateElapsed = 0.0f;
        nextBlinkTime = randomBlinkInterval();
    }

    public void update(
            CubismModel model,
            float deltaTimeSeconds
    ) {
        if (model == null) {
            return;
        }

        deltaTimeSeconds = Math.max(
                0.0f,
                Math.min(deltaTimeSeconds, 0.1f)
        );

        stateElapsed += deltaTimeSeconds;

        float eyeOpenValue = 1.0f;

        switch (state) {
            case WAITING:
                if (stateElapsed >= nextBlinkTime) {
                    state = BlinkState.CLOSING;
                    stateElapsed = 0.0f;
                }
                break;

            case CLOSING:
                eyeOpenValue = 1.0f - smoothStep(
                        stateElapsed / CLOSING_DURATION
                );

                if (stateElapsed >= CLOSING_DURATION) {
                    state = BlinkState.CLOSED;
                    stateElapsed = 0.0f;
                    eyeOpenValue = 0.0f;
                }
                break;

            case CLOSED:
                eyeOpenValue = 0.0f;

                if (stateElapsed >= CLOSED_DURATION) {
                    state = BlinkState.OPENING;
                    stateElapsed = 0.0f;
                }
                break;

            case OPENING:
                eyeOpenValue = smoothStep(
                        stateElapsed / OPENING_DURATION
                );

                if (stateElapsed >= OPENING_DURATION) {
                    state = BlinkState.WAITING;
                    stateElapsed = 0.0f;
                    nextBlinkTime = randomBlinkInterval();
                    eyeOpenValue = 1.0f;
                }
                break;
        }

        model.setParameterValue(
                idParamEyeLOpen,
                eyeOpenValue
        );

        model.setParameterValue(
                idParamEyeROpen,
                eyeOpenValue
        );
    }

    private static float randomBlinkInterval() {
        return MIN_BLINK_INTERVAL
                + (float) Math.random()
                * (MAX_BLINK_INTERVAL - MIN_BLINK_INTERVAL);
    }

    private static float smoothStep(float value) {
        value = Math.max(
                0.0f,
                Math.min(value, 1.0f)
        );

        return value * value * (3.0f - 2.0f * value);
    }
}