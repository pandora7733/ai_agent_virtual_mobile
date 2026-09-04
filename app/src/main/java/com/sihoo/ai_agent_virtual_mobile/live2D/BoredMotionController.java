package com.sihoo.ai_agent_virtual_mobile.live2D;

import com.live2d.sdk.cubism.framework.id.CubismId;
import com.live2d.sdk.cubism.framework.model.CubismModel;

public class BoredMotionController {
    private static final float MOTION_DURATION = 4.0f;

    private static final float LOOK_LEFT_OFFSET = -5.0f;
    private static final float LOOK_RIGHT_OFFSET = 4.0f;
    private static final float HEAD_DOWN_OFFSET = -2.0f;
    private static final float HEAD_TILT_OFFSET = 1.2f;
    private static final float BODY_OFFSET = 1.5f;

    private final CubismId idParamAngleX;
    private final CubismId idParamAngleY;
    private final CubismId idParamAngleZ;
    private final CubismId idParamBodyAngleX;

    private float elapsed = 0.0f;
    private boolean active = false;

    private float baseAngleX;
    private float baseAngleY;
    private float baseAngleZ;
    private float baseBodyAngleX;

    public BoredMotionController(
            CubismId idParamAngleX,
            CubismId idParamAngleY,
            CubismId idParamAngleZ,
            CubismId idParamBodyAngleX
    ) {
        this.idParamAngleX = idParamAngleX;
        this.idParamAngleY = idParamAngleY;
        this.idParamAngleZ = idParamAngleZ;
        this.idParamBodyAngleX = idParamBodyAngleX;
    }

    public boolean start(CubismModel model) {
        if (model == null) {
            active = false;
            return false;
        }

        baseAngleX = model.getParameterValue(idParamAngleX);
        baseAngleY = model.getParameterValue(idParamAngleY);
        baseAngleZ = model.getParameterValue(idParamAngleZ);
        baseBodyAngleX = model.getParameterValue(idParamBodyAngleX);

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

        float offsetX = 0.0f;
        float offsetY = 0.0f;
        float offsetZ = 0.0f;
        float bodyOffsetX = 0.0f;

        if (elapsed < 0.6f) {
            // 지루한 듯 고개를 살짝 숙임
            float t = smoothStep(elapsed / 0.6f);
            offsetY = HEAD_DOWN_OFFSET * t;
            offsetZ = HEAD_TILT_OFFSET * t;

        } else if (elapsed < 1.4f) {
            // 천천히 왼쪽을 바라봄
            float t = smoothStep(
                    (elapsed - 0.6f) / 0.8f
            );
            offsetX = lerp(0.0f, LOOK_LEFT_OFFSET, t);
            offsetY = HEAD_DOWN_OFFSET;
            offsetZ = HEAD_TILT_OFFSET;
            bodyOffsetX = BODY_OFFSET * t;

        } else if (elapsed < 2.2f) {
            // 반대쪽을 천천히 바라봄
            float t = smoothStep(
                    (elapsed - 1.4f) / 0.8f
            );
            offsetX = lerp(
                    LOOK_LEFT_OFFSET,
                    LOOK_RIGHT_OFFSET,
                    t
            );
            offsetY = HEAD_DOWN_OFFSET;
            offsetZ = lerp(
                    HEAD_TILT_OFFSET,
                    -HEAD_TILT_OFFSET,
                    t
            );
            bodyOffsetX = BODY_OFFSET * (1.0f - t);

        } else if (elapsed < 3.0f) {
            // 다시 정면으로 돌아옴
            float t = smoothStep(
                    (elapsed - 2.2f) / 0.8f
            );
            offsetX = lerp(LOOK_RIGHT_OFFSET, 0.0f, t);
            offsetY = HEAD_DOWN_OFFSET * (1.0f - t);
            offsetZ = lerp(-HEAD_TILT_OFFSET, 0.0f, t);

        } else {
            // 마지막 자세를 안정시키며 기본 자세로 복귀
            float t = smoothStep(
                    (elapsed - 3.0f) / 1.0f
            );
            offsetX = 0.0f;
            offsetY = 0.0f;
            offsetZ = 0.0f;
            bodyOffsetX = 0.0f * t;
        }

        model.setParameterValue(
                idParamAngleX,
                baseAngleX + offsetX
        );
        model.setParameterValue(
                idParamAngleY,
                baseAngleY + offsetY
        );
        model.setParameterValue(
                idParamAngleZ,
                baseAngleZ + offsetZ
        );
        model.setParameterValue(
                idParamBodyAngleX,
                baseBodyAngleX + bodyOffsetX
        );

        if (elapsed >= MOTION_DURATION) {
            applyBasePose(model);
            active = false;
            return true;
        }

        return false;
    }

    private void applyBasePose(CubismModel model) {
        model.setParameterValue(idParamAngleX, baseAngleX);
        model.setParameterValue(idParamAngleY, baseAngleY);
        model.setParameterValue(idParamAngleZ, baseAngleZ);
        model.setParameterValue(idParamBodyAngleX, baseBodyAngleX);
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
        return Math.max(
                0.0f,
                Math.min(value, 1.0f)
        );
    }
}
