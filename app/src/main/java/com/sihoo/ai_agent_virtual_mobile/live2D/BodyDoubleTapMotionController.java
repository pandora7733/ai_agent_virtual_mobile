package com.sihoo.ai_agent_virtual_mobile.live2D;

import com.live2d.sdk.cubism.framework.CubismFramework;
import com.live2d.sdk.cubism.framework.id.CubismId;
import com.live2d.sdk.cubism.framework.model.CubismModel;

public class BodyDoubleTapMotionController {
    private static final float MOTION_DURATION = 0.9f;
    private static final float RECOIL_END = 0.18f;
    private static final float FORWARD_END = 0.50f;
    private static final float RETURN_END = 0.85f;

    private static final float RECOIL_BODY_OFFSET = -3.5f;
    private static final float FORWARD_BODY_OFFSET = 2.5f;
    private static final float RECOIL_ANGLE_Y = -2.0f;
    private static final float BLUSH_INTENSITY = 0.15f;

    private final CubismId idParamAngleY;
    private final CubismId idParamBodyAngleX;
    private final CubismId idExpFaceBlush;

    private boolean active = false;
    private float elapsed = 0.0f;
    private float baseAngleY;
    private float baseBodyAngleX;
    private float baseFaceBlush;

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
        if (model == null) {
            active = false;
            return false;
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
            float t = smoothStep(elapsed / RECOIL_END);
            bodyOffset = lerp(0.0f, RECOIL_BODY_OFFSET, t);
            angleYOffset = lerp(0.0f, RECOIL_ANGLE_Y, t);
            blush = lerp(0.0f, BLUSH_INTENSITY, t);
        } else if (elapsed < FORWARD_END) {
            float t = smoothStep(
                    (elapsed - RECOIL_END) / (FORWARD_END - RECOIL_END)
            );
            bodyOffset = lerp(RECOIL_BODY_OFFSET, FORWARD_BODY_OFFSET, t);
            angleYOffset = lerp(RECOIL_ANGLE_Y, 0.0f, t);
            blush = BLUSH_INTENSITY;
        } else if (elapsed < RETURN_END) {
            float t = smoothStep(
                    (elapsed - FORWARD_END) / (RETURN_END - FORWARD_END)
            );
            bodyOffset = lerp(FORWARD_BODY_OFFSET, 0.0f, t);
            angleYOffset = 0.0f;
            blush = lerp(BLUSH_INTENSITY, 0.0f, t);
        } else {
            bodyOffset = 0.0f;
            angleYOffset = 0.0f;
            blush = 0.0f;
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

    private void applyBasePose(CubismModel model) {
        model.setParameterValue(idParamBodyAngleX, baseBodyAngleX);
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
