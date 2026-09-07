package com.sihoo.ai_agent_virtual_mobile.live2D;

import com.live2d.sdk.cubism.framework.model.CubismModel;

/**
 * 모델 파라미터 전체를 시작값에서 목표값으로 부드럽게 보간한다.
 */
public class ParameterTransitionController {
    private float durationSeconds = 1.0f;
    private float elapsed = 0.0f;
    private boolean active = false;

    private float[] startValues;
    private float[] targetValues;

    public boolean start(
            CubismModel model,
            float[] target,
            float durationSeconds
    ) {
        if (model == null || target == null || durationSeconds <= 0.0f) {
            active = false;
            return false;
        }

        int parameterCount = model.getParameterCount();
        if (target.length < parameterCount) {
            active = false;
            return false;
        }

        startValues = new float[parameterCount];
        targetValues = new float[parameterCount];

        for (int i = 0; i < parameterCount; i++) {
            startValues[i] = model.getParameterValue(i);
            targetValues[i] = target[i];
        }

        this.durationSeconds = durationSeconds;
        elapsed = 0.0f;
        active = true;
        return true;
    }

    public void stop(CubismModel model) {
        if (model != null && active && targetValues != null) {
            applyTarget(model);
        }
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFinished() {
        return !active;
    }

    public boolean update(
            CubismModel model,
            float deltaTimeSeconds
    ) {
        if (!active || model == null) {
            return true;
        }

        deltaTimeSeconds = Math.max(
                0.0f,
                Math.min(deltaTimeSeconds, 0.1f)
        );
        elapsed += deltaTimeSeconds;

        float progress = smoothStep(elapsed / durationSeconds);
        int parameterCount = model.getParameterCount();

        for (int i = 0; i < parameterCount; i++) {
            model.setParameterValue(
                    i,
                    lerp(startValues[i], targetValues[i], progress)
            );
        }

        if (elapsed >= durationSeconds) {
            applyTarget(model);
            active = false;
            return true;
        }

        return false;
    }

    private void applyTarget(CubismModel model) {
        int parameterCount = Math.min(
                model.getParameterCount(),
                targetValues.length
        );

        for (int i = 0; i < parameterCount; i++) {
            model.setParameterValue(i, targetValues[i]);
        }
    }

    private static float lerp(
            float start,
            float end,
            float amount
    ) {
        return start + (end - start) * normalize(amount);
    }

    private static float smoothStep(float value) {
        value = normalize(value);
        return value * value * (3.0f - 2.0f * value);
    }

    private static float normalize(float value) {
        return Math.max(0.0f, Math.min(value, 1.0f));
    }
}
