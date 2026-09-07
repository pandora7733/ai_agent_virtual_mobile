package com.sihoo.ai_agent_virtual_mobile.live2D;

import com.live2d.sdk.cubism.framework.id.CubismId;
import com.live2d.sdk.cubism.framework.model.CubismModel;

public class FirstVisitMotionController {
    private static final float MOTION_DURATION = 3.2f;

    // 잠든 동안 고개를 숙이는 정도
    private static final float HEAD_SLEEP_OFFSET = 3.0f;

    // 좌우로 바라보는 정도
    private static final float HEAD_X_OFFSET = 17.5f;

    // 고개 기울기 정도
    private static final float HEAD_Z_OFFSET = 5.7f;

    // 몸 흔들림 정도
    private static final float BODY_X_OFFSET = 5.0f;

    private final CubismId idParamAngleX;
    private final CubismId idParamAngleY;
    private final CubismId idParamAngleZ;
    private final CubismId idParamBodyAngleX;
    private final CubismId idParamEyeBallX;
    private final CubismId idParamEyeBallY;
    private final CubismId idParamEyeLOpen;
    private final CubismId idParamEyeROpen;

    private float elapsed = 0.0f;
    private boolean active = false;

    private float baseAngleX;
    private float baseAngleY;
    private float baseAngleZ;
    private float baseBodyAngleX;
    private float baseEyeBallX;
    private float baseEyeBallY;
    private float baseEyeLOpen;
    private float baseEyeROpen;

    public FirstVisitMotionController(
            CubismId idParamAngleX,
            CubismId idParamAngleY,
            CubismId idParamAngleZ,
            CubismId idParamBodyAngleX,
            CubismId idParamEyeBallX,
            CubismId idParamEyeBallY,
            CubismId idParamEyeLOpen,
            CubismId idParamEyeROpen
    ) {
        this.idParamAngleX = idParamAngleX;
        this.idParamAngleY = idParamAngleY;
        this.idParamAngleZ = idParamAngleZ;
        this.idParamBodyAngleX = idParamBodyAngleX;
        this.idParamEyeBallX = idParamEyeBallX;
        this.idParamEyeBallY = idParamEyeBallY;
        this.idParamEyeLOpen = idParamEyeLOpen;
        this.idParamEyeROpen = idParamEyeROpen;
    }

    public void start(CubismModel model) {
        if (model == null) {
            active = false;
            return;
        }

        // 모션 시작 당시의 현재 자세를 기준값으로 저장
        baseAngleX = model.getParameterValue(idParamAngleX);
        baseAngleY = model.getParameterValue(idParamAngleY);
        baseAngleZ = model.getParameterValue(idParamAngleZ);
        baseBodyAngleX = model.getParameterValue(idParamBodyAngleX);
        baseEyeBallX = model.getParameterValue(idParamEyeBallX);
        baseEyeBallY = model.getParameterValue(idParamEyeBallY);
        baseEyeLOpen = model.getParameterValue(idParamEyeLOpen);
        baseEyeROpen = model.getParameterValue(idParamEyeROpen);

        elapsed = 0.0f;
        active = true;
    }

    public void stop() {
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

        if (deltaTimeSeconds < 0.0f) {
            deltaTimeSeconds = 0.0f;
        }

        deltaTimeSeconds = Math.min(deltaTimeSeconds, 0.1f);
        elapsed += deltaTimeSeconds;

        float offsetX = 0.0f;
        float offsetY = 0.0f;
        float offsetZ = 0.0f;
        float bodyOffsetX = 0.0f;
        float eyeOpenLeft = 1.0f;
        float eyeOpenRight = 1.0f;

        if (elapsed < 0.35f) {
            // 눈을 감고 잠든 자세로 들어감
            float t = smoothStep(elapsed / 0.35f);

            offsetY = -HEAD_SLEEP_OFFSET * t;
            eyeOpenLeft = lerp(baseEyeLOpen, 0.0f, t);
            eyeOpenRight = lerp(baseEyeROpen, 0.0f, t);

        } else if (elapsed < 1.95f) {
            // 눈을 감은 상태로 좌우로 머리를 흔듦
            float t = normalize(
                    (elapsed - 0.35f) / 1.6f
            );

            float envelope = (float) Math.sin(t * Math.PI);
            envelope *= envelope;

            offsetX = HEAD_X_OFFSET
                    * envelope
                    * (float) Math.sin(t * Math.PI * 4.0f);

            offsetY = -HEAD_SLEEP_OFFSET;

            offsetZ = HEAD_Z_OFFSET
                    * envelope
                    * (float) Math.sin(
                    t * Math.PI * 4.0f + Math.PI * 0.5f
            );

            bodyOffsetX = BODY_X_OFFSET
                    * envelope
                    * (float) Math.sin(t * Math.PI * 2.0f);

            eyeOpenLeft = 0.0f;
            eyeOpenRight = 0.0f;

        } else {
            // 기본 자세로 돌아오면서 눈을 뜨고 한 번 깜빡임
            float t = normalize(
                    (elapsed - 1.95f) / 1.25f
            );
            float remain = 1.0f - smoothStep(t);

            offsetY = -HEAD_SLEEP_OFFSET * remain;
            eyeOpenLeft = wakeEyeValue(t, baseEyeLOpen);
            eyeOpenRight = wakeEyeValue(t, baseEyeROpen);
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

        // 눈동자는 아직 원래 위치 유지
        model.setParameterValue(
                idParamEyeBallX,
                baseEyeBallX
        );

        model.setParameterValue(
                idParamEyeBallY,
                baseEyeBallY
        );

        model.setParameterValue(
                idParamEyeLOpen,
                eyeOpenLeft
        );

        model.setParameterValue(
                idParamEyeROpen,
                eyeOpenRight
        );

        if (elapsed >= MOTION_DURATION) {
            model.setParameterValue(idParamAngleX, baseAngleX);
            model.setParameterValue(idParamAngleY, baseAngleY);
            model.setParameterValue(idParamAngleZ, baseAngleZ);
            model.setParameterValue(idParamBodyAngleX, baseBodyAngleX);
            model.setParameterValue(idParamEyeLOpen, baseEyeLOpen);
            model.setParameterValue(idParamEyeROpen, baseEyeROpen);

            active = false;
            return true;
        }

        return false;
    }

    private static float normalize(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static float smoothStep(float value) {
        value = normalize(value);
        return value * value * (3.0f - 2.0f * value);
    }

    private static float lerp(
            float start,
            float end,
            float amount
    ) {
        return start + (end - start) * normalize(amount);
    }

    private static float wakeEyeValue(
            float value,
            float openValue
    ) {
        value = normalize(value);

        if (value < 0.2f) {
            return 0.0f;
        }

        if (value < 0.38f) {
            return lerp(
                    0.0f,
                    openValue,
                    smoothStep((value - 0.2f) / 0.18f)
            );
        }

        if (value < 0.50f) {
            return lerp(
                    openValue,
                    0.0f,
                    smoothStep((value - 0.38f) / 0.12f)
            );
        }

        if (value < 0.75f) {
            return lerp(
                    0.0f,
                    openValue,
                    smoothStep((value - 0.50f) / 0.25f)
            );
        }

        return openValue;
    }

    public float getElapsed() {
        return elapsed;
    }
}