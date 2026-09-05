package com.sihoo.ai_agent_virtual_mobile.live2D;

import com.live2d.sdk.cubism.framework.CubismFramework;
import com.live2d.sdk.cubism.framework.id.CubismId;
import com.live2d.sdk.cubism.framework.model.CubismModel;

public class BodyStrokeMotionController {
    private static final float BODY_X_SCALE = 8.0f;
    private static final float ANGLE_X_SCALE = 3.5f;
    private static final float ANGLE_Z_SCALE = 2.0f;
    private static final float BLUSH_INTENSITY = 0.22f;
    private static final float FOLLOW_SPEED = 10.0f;
    private static final float RELEASE_DURATION = 0.4f;

    private enum Phase {
        FOLLOWING,
        RELEASING
    }

    private final CubismId idParamAngleX;
    private final CubismId idParamAngleZ;
    private final CubismId idParamBodyAngleX;
    private final CubismId idExpFaceBlush;

    private boolean active = false;
    private Phase phase = Phase.FOLLOWING;

    private float baseAngleX;
    private float baseAngleZ;
    private float baseBodyAngleX;
    private float baseFaceBlush;

    private float currentBodyX;
    private float currentAngleX;
    private float currentAngleZ;
    private float currentBlush;

    private float targetBodyX;
    private float targetAngleX;
    private float targetAngleZ;

    private float releaseStartBodyX;
    private float releaseStartAngleX;
    private float releaseStartAngleZ;
    private float releaseStartBlush;
    private float releaseElapsed;

    public BodyStrokeMotionController(
            CubismId idParamAngleX,
            CubismId idParamAngleZ,
            CubismId idParamBodyAngleX
    ) {
        this.idParamAngleX = idParamAngleX;
        this.idParamAngleZ = idParamAngleZ;
        this.idParamBodyAngleX = idParamBodyAngleX;
        this.idExpFaceBlush = CubismFramework.getIdManager()
                .getId("ExpFaceBlush");
    }

    public boolean start(CubismModel model) {
        if (model == null) {
            active = false;
            return false;
        }

        baseAngleX = model.getParameterValue(idParamAngleX);
        baseAngleZ = model.getParameterValue(idParamAngleZ);
        baseBodyAngleX = model.getParameterValue(idParamBodyAngleX);
        baseFaceBlush = model.getParameterValue(idExpFaceBlush);

        currentBodyX = 0.0f;
        currentAngleX = 0.0f;
        currentAngleZ = 0.0f;
        currentBlush = 0.0f;
        targetBodyX = 0.0f;
        targetAngleX = 0.0f;
        targetAngleZ = 0.0f;
        releaseElapsed = 0.0f;
        phase = Phase.FOLLOWING;
        active = true;
        return true;
    }

    public void setTarget(float strokeX, float strokeY) {
        if (!active || phase != Phase.FOLLOWING) {
            return;
        }

        float x = clamp(strokeX, -1.0f, 1.0f);
        targetBodyX = x * BODY_X_SCALE;
        targetAngleX = x * ANGLE_X_SCALE;
        targetAngleZ = x * ANGLE_Z_SCALE;
    }

    public void startRelease() {
        if (!active) {
            return;
        }

        phase = Phase.RELEASING;
        releaseElapsed = 0.0f;
        releaseStartBodyX = currentBodyX;
        releaseStartAngleX = currentAngleX;
        releaseStartAngleZ = currentAngleZ;
        releaseStartBlush = currentBlush;
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

    public boolean isReleasing() {
        return active && phase == Phase.RELEASING;
    }

    public boolean update(CubismModel model, float deltaTimeSeconds) {
        if (!active || model == null) {
            return true;
        }

        deltaTimeSeconds = Math.max(
                0.0f,
                Math.min(deltaTimeSeconds, 0.1f)
        );

        if (phase == Phase.FOLLOWING) {
            float followT = 1.0f - (float) Math.exp(
                    -FOLLOW_SPEED * deltaTimeSeconds
            );
            currentBodyX = lerp(currentBodyX, targetBodyX, followT);
            currentAngleX = lerp(currentAngleX, targetAngleX, followT);
            currentAngleZ = lerp(currentAngleZ, targetAngleZ, followT);
            currentBlush = lerp(currentBlush, BLUSH_INTENSITY, followT);
        } else {
            releaseElapsed += deltaTimeSeconds;
            float t = smoothStep(releaseElapsed / RELEASE_DURATION);
            currentBodyX = lerp(releaseStartBodyX, 0.0f, t);
            currentAngleX = lerp(releaseStartAngleX, 0.0f, t);
            currentAngleZ = lerp(releaseStartAngleZ, 0.0f, t);
            currentBlush = lerp(releaseStartBlush, 0.0f, t);

            if (releaseElapsed >= RELEASE_DURATION) {
                applyBasePose(model);
                active = false;
                return true;
            }
        }

        model.setParameterValue(
                idParamBodyAngleX,
                baseBodyAngleX + currentBodyX
        );
        model.setParameterValue(idParamAngleX, baseAngleX + currentAngleX);
        model.setParameterValue(idParamAngleZ, baseAngleZ + currentAngleZ);
        model.setParameterValue(idExpFaceBlush, baseFaceBlush + currentBlush);
        return false;
    }

    private void applyBasePose(CubismModel model) {
        model.setParameterValue(idParamBodyAngleX, baseBodyAngleX);
        model.setParameterValue(idParamAngleX, baseAngleX);
        model.setParameterValue(idParamAngleZ, baseAngleZ);
        model.setParameterValue(idExpFaceBlush, baseFaceBlush);
    }

    private static float lerp(float start, float end, float amount) {
        return start + (end - start) * clamp(amount, 0.0f, 1.0f);
    }

    private static float smoothStep(float value) {
        value = clamp(value, 0.0f, 1.0f);
        return value * value * (3.0f - 2.0f * value);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
