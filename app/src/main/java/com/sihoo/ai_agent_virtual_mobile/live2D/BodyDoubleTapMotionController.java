package com.sihoo.ai_agent_virtual_mobile.live2D;

import com.live2d.sdk.cubism.framework.CubismFramework;
import com.live2d.sdk.cubism.framework.id.CubismId;
import com.live2d.sdk.cubism.framework.model.CubismModel;

public class BodyDoubleTapMotionController {
    private static final float MOTION_DURATION = 1.18f;
    private static final float RECOIL_END = 0.32f;
    private static final float OVERSHOOT_END = 0.74f;
    private static final float RETURN_END = 1.10f;
    private static final float BLUSH_FADE_START = 0.72f;

    private final CubismId idParamAngleY;
    private final CubismId idParamBodyAngleX;
    private final CubismId idExpFaceBlush;

    private boolean active = false;
    private float elapsed = 0.0f;
    private float baseAngleY;
    private float baseBodyAngleX;
    private float baseFaceBlush;
    private float recoilBodyOffset = -2.4f;
    private float overshootBodyOffset = 1.1f;
    private float recoilAngleY = -1.6f;
    private float blushIntensity = 0.15f;

    public BodyDoubleTapMotionController(
            CubismId idParamAngleY,
            CubismId idParamBodyAngleX
    ) {
        this.idParamAngleY = idParamAngleY;
        this.idParamBodyAngleX = idParamBodyAngleX;
        this.idExpFaceBlush = CubismFramework.getIdManager()
                .getId("ExpFaceBlush");
    }

    public boolean start(CubismModel model) {
        return start(model, false);
    }

    public boolean start(CubismModel model, boolean chest) {
        if (model == null) {
            active = false;
            return false;
        }

        if (chest) {
            recoilBodyOffset = -3.2f;
            overshootBodyOffset = 0.7f;
            recoilAngleY = -2.2f;
            blushIntensity = 0.12f;
        } else {
            recoilBodyOffset = -1.8f;
            overshootBodyOffset = 1.5f;
            recoilAngleY = -0.6f;
            blushIntensity = 0.22f;
        }

        baseAngleY = model.getParameterValue(idParamAngleY);
        baseBodyAngleX = model.getParameterValue(idParamBodyAngleX);
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

        float bodyOffset;
        float angleYOffset;
        float blush;

        if (elapsed < RECOIL_END) {
            float t = easeOutCubic(elapsed / RECOIL_END);
            bodyOffset = lerp(0.0f, recoilBodyOffset, t);
            angleYOffset = lerp(0.0f, recoilAngleY, t);
            blush = lerp(0.0f, blushIntensity, smootherStep(elapsed / RECOIL_END));
        } else if (elapsed < OVERSHOOT_END) {
            float t = easeInOutCubic(
                    (elapsed - RECOIL_END) / (OVERSHOOT_END - RECOIL_END)
            );
            bodyOffset = lerp(recoilBodyOffset, overshootBodyOffset, t);
            angleYOffset = lerp(recoilAngleY, 0.0f, t);
            blush = blushIntensity;
        } else if (elapsed < RETURN_END) {
            float t = easeInOutCubic(
                    (elapsed - OVERSHOOT_END) / (RETURN_END - OVERSHOOT_END)
            );
            bodyOffset = lerp(overshootBodyOffset, 0.0f, t);
            angleYOffset = 0.0f;
            blush = blushAfterHold(elapsed);
        } else {
            bodyOffset = 0.0f;
            angleYOffset = 0.0f;
            blush = blushAfterHold(elapsed);
        }

        model.setParameterValue(
                idParamBodyAngleX,
                baseBodyAngleX + bodyOffset
        );
        model.setParameterValue(idParamAngleY, baseAngleY + angleYOffset);
        model.setParameterValue(idExpFaceBlush, baseFaceBlush + blush);

        if (elapsed >= MOTION_DURATION) {
            applyBasePose(model);
            active = false;
            return true;
        }

        return false;
    }

    private float blushAfterHold(float time) {
        if (time <= BLUSH_FADE_START) {
            return blushIntensity;
        }

        return lerp(
                blushIntensity,
                0.0f,
                smootherStep(
                        (time - BLUSH_FADE_START)
                                / (MOTION_DURATION - BLUSH_FADE_START)
                )
        );
    }

    private void applyBasePose(CubismModel model) {
        model.setParameterValue(idParamBodyAngleX, baseBodyAngleX);
        model.setParameterValue(idParamAngleY, baseAngleY);
        model.setParameterValue(idExpFaceBlush, baseFaceBlush);
    }

    private static float lerp(float start, float end, float amount) {
        amount = clamp01(amount);
        return start + (end - start) * amount;
    }

    private static float easeOutCubic(float value) {
        value = clamp01(value);
        float inverse = 1.0f - value;
        return 1.0f - inverse * inverse * inverse;
    }

    private static float easeInOutCubic(float value) {
        value = clamp01(value);
        if (value < 0.5f) {
            return 4.0f * value * value * value;
        }

        float inverse = -2.0f * value + 2.0f;
        return 1.0f - (inverse * inverse * inverse) / 2.0f;
    }

    private static float smootherStep(float value) {
        value = clamp01(value);
        return value * value * value * (value * (value * 6.0f - 15.0f) + 10.0f);
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(value, 1.0f));
    }
}
