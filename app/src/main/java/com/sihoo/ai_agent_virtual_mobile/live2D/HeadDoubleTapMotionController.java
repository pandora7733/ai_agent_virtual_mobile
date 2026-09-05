package com.sihoo.ai_agent_virtual_mobile.live2D;

import com.live2d.sdk.cubism.framework.CubismFramework;
import com.live2d.sdk.cubism.framework.id.CubismId;
import com.live2d.sdk.cubism.framework.model.CubismModel;

public class HeadDoubleTapMotionController {
    private static final float MOTION_DURATION = 0.9f;
    private static final float NOD_DOWN_END = 0.15f;
    private static final float NOD_UP_END = 0.45f;
    private static final float RETURN_END = 0.80f;

    private static final float NOD_DOWN_OFFSET = -4.5f;
    private static final float NOD_UP_OFFSET = 2.0f;
    private static final float BLUSH_INTENSITY = 0.20f;

    private final CubismId idParamAngleY;
    private final CubismId idExpFaceBlush;

    private boolean active = false;
    private float elapsed = 0.0f;
    private float baseAngleY;
    private float baseFaceBlush;

    public HeadDoubleTapMotionController(CubismId idParamAngleY) {
        this.idParamAngleY = idParamAngleY;
        this.idExpFaceBlush = CubismFramework.getIdManager()
                .getId("ExpFaceBlush");
    }

    public boolean start(CubismModel model) {
        if (model == null) {
            active = false;
            return false;
        }

        baseAngleY = model.getParameterValue(idParamAngleY);
        baseFaceBlush = model.getParameterValue(idExpFaceBlush);
        elapsed = 0.0f;
        active = true;
        return true;
    }

    public void stop(CubismModel model) {
        if (model != null && active) {
            applyBasePose(model);
        }
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isFinished() {
        return !active;
    }

    public boolean update(CubismModel model, float deltaTimeSeconds) {
        if (!active || model == null) {
            return true;
        }

        deltaTimeSeconds = Math.max(
                0.0f,
                Math.min(deltaTimeSeconds, 0.1f)
        );
        elapsed += deltaTimeSeconds;

        float offsetY;
        float blush;

        if (elapsed < NOD_DOWN_END) {
            float t = smoothStep(elapsed / NOD_DOWN_END);
            offsetY = lerp(0.0f, NOD_DOWN_OFFSET, t);
            blush = lerp(0.0f, BLUSH_INTENSITY, t);
        } else if (elapsed < NOD_UP_END) {
            float t = smoothStep(
                    (elapsed - NOD_DOWN_END) / (NOD_UP_END - NOD_DOWN_END)
            );
            offsetY = lerp(NOD_DOWN_OFFSET, NOD_UP_OFFSET, t);
            blush = BLUSH_INTENSITY;
        } else if (elapsed < RETURN_END) {
            float t = smoothStep(
                    (elapsed - NOD_UP_END) / (RETURN_END - NOD_UP_END)
            );
            offsetY = lerp(NOD_UP_OFFSET, 0.0f, t);
            blush = lerp(BLUSH_INTENSITY, 0.0f, t);
        } else {
            offsetY = 0.0f;
            blush = 0.0f;
        }

        model.setParameterValue(idParamAngleY, baseAngleY + offsetY);
        model.setParameterValue(idExpFaceBlush, baseFaceBlush + blush);

        if (elapsed >= MOTION_DURATION) {
            applyBasePose(model);
            active = false;
            return true;
        }

        return false;
    }

    private void applyBasePose(CubismModel model) {
        model.setParameterValue(idParamAngleY, baseAngleY);
        model.setParameterValue(idExpFaceBlush, baseFaceBlush);
    }

    private static float lerp(float start, float end, float amount) {
        amount = Math.max(0.0f, Math.min(amount, 1.0f));
        return start + (end - start) * amount;
    }

    private static float smoothStep(float value) {
        value = Math.max(0.0f, Math.min(value, 1.0f));
        return value * value * (3.0f - 2.0f * value);
    }
}
