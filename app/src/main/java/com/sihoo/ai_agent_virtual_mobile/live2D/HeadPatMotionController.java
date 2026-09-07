package com.sihoo.ai_agent_virtual_mobile.live2D;

import com.live2d.sdk.cubism.framework.CubismFramework;
import com.live2d.sdk.cubism.framework.id.CubismId;
import com.live2d.sdk.cubism.framework.model.CubismModel;

public class HeadPatMotionController {
    private static final float ANGLE_X_SCALE = 10.0f;
    private static final float ANGLE_Y_SCALE = 5.0f;
    private static final float ANGLE_Z_SCALE = 4.0f;
    private static final float BLUSH_INTENSITY = 0.35f;
    private static final float FOLLOW_SPEED = 10.0f;
    private static final float RELEASE_DURATION = 0.4f;

    private enum Phase {
        FOLLOWING,
        RELEASING
    }

    private final CubismId idParamAngleX;
    private final CubismId idParamAngleY;
    private final CubismId idParamAngleZ;
    private final CubismId idExpFaceBlush;

    private boolean active = false;
    private Phase phase = Phase.FOLLOWING;

    private float baseAngleX;
    private float baseAngleY;
    private float baseAngleZ;
    private float baseFaceBlush;

    private float currentOffsetX;
    private float currentOffsetY;
    private float currentOffsetZ;
    private float currentBlush;

    private float targetOffsetX;
    private float targetOffsetY;
    private float targetOffsetZ;

    private float releaseStartX;
    private float releaseStartY;
    private float releaseStartZ;
    private float releaseStartBlush;
    private float releaseElapsed;

    public HeadPatMotionController(
            CubismId idParamAngleX,
            CubismId idParamAngleY,
            CubismId idParamAngleZ
    ) {
        this.idParamAngleX = idParamAngleX;
        this.idParamAngleY = idParamAngleY;
        this.idParamAngleZ = idParamAngleZ;
        this.idExpFaceBlush = CubismFramework.getIdManager()
                .getId("ExpFaceBlush");
    }

    public boolean start(CubismModel model) {
        if (model == null) {
            active = false;
            return false;
        }

        baseAngleX = model.getParameterValue(idParamAngleX);
        baseAngleY = model.getParameterValue(idParamAngleY);
        baseAngleZ = model.getParameterValue(idParamAngleZ);
        baseFaceBlush = model.getParameterValue(idExpFaceBlush);

        currentOffsetX = 0.0f;
        currentOffsetY = 0.0f;
        currentOffsetZ = 0.0f;
        currentBlush = 0.0f;
        targetOffsetX = 0.0f;
        targetOffsetY = 0.0f;
        targetOffsetZ = 0.0f;
        releaseElapsed = 0.0f;
        phase = Phase.FOLLOWING;
        active = true;
        return true;
    }

    public void setTarget(float patX, float patY) {
        if (!active || phase != Phase.FOLLOWING) {
            return;
        }

        float x = clamp(patX, -1.0f, 1.0f);
        float y = clamp(patY, -1.0f, 1.0f);
        targetOffsetX = x * ANGLE_X_SCALE;
        targetOffsetY = y * ANGLE_Y_SCALE;
        targetOffsetZ = x * ANGLE_Z_SCALE;
    }

    public void startRelease() {
        if (!active) {
            return;
        }

        phase = Phase.RELEASING;
        releaseElapsed = 0.0f;
        releaseStartX = currentOffsetX;
        releaseStartY = currentOffsetY;
        releaseStartZ = currentOffsetZ;
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
            currentOffsetX = lerp(currentOffsetX, targetOffsetX, followT);
            currentOffsetY = lerp(currentOffsetY, targetOffsetY, followT);
            currentOffsetZ = lerp(currentOffsetZ, targetOffsetZ, followT);
            currentBlush = lerp(currentBlush, BLUSH_INTENSITY, followT);
        } else {
            releaseElapsed += deltaTimeSeconds;
            float t = smoothStep(releaseElapsed / RELEASE_DURATION);
            currentOffsetX = lerp(releaseStartX, 0.0f, t);
            currentOffsetY = lerp(releaseStartY, 0.0f, t);
            currentOffsetZ = lerp(releaseStartZ, 0.0f, t);
            currentBlush = lerp(releaseStartBlush, 0.0f, t);

            if (releaseElapsed >= RELEASE_DURATION) {
                applyBasePose(model);
                active = false;
                return true;
            }
        }

        model.setParameterValue(idParamAngleX, baseAngleX + currentOffsetX);
        model.setParameterValue(idParamAngleY, baseAngleY + currentOffsetY);
        model.setParameterValue(idParamAngleZ, baseAngleZ + currentOffsetZ);
        model.setParameterValue(idExpFaceBlush, baseFaceBlush + currentBlush);
        return false;
    }

    private void applyBasePose(CubismModel model) {
        model.setParameterValue(idParamAngleX, baseAngleX);
        model.setParameterValue(idParamAngleY, baseAngleY);
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
