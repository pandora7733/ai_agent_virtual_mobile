package com.sihoo.ai_agent_virtual_mobile.live2D;

import com.live2d.sdk.cubism.framework.CubismFramework;
import com.live2d.sdk.cubism.framework.id.CubismId;
import com.live2d.sdk.cubism.framework.model.CubismModel;

public class BoredMotionController {
    // ============================================================
    // 시간[초] — 조절 완료된 최종 고정값 (직접 수정)
    // ============================================================
    private static final float PHASE_HEAD_DOWN_END = 0.6f;

    /** 좌우 1방향(왼쪽 또는 오른쪽)으로 움직이는 시간. 클수록 느림 */
    private static final float SWAY_HALF_SEGMENT_DURATION = 0.68f;

    /** 좌우 흔들림 횟수 (왼쪽↔오른쪽 1회 = 1) */
    private static final int SWAY_CYCLE_COUNT = 2;

    /**
     * 흔들림 구간 전체 길이.
     * SWAY_HALF_SEGMENT_DURATION × SWAY_CYCLE_COUNT × 2 와 반드시 일치해야 함.
     */
    private static final float SWAY_TOTAL_DURATION =
            SWAY_HALF_SEGMENT_DURATION * SWAY_CYCLE_COUNT * 2;

    private static final float PHASE_SWAY_END =
            PHASE_HEAD_DOWN_END + SWAY_TOTAL_DURATION;

    private static final float RETURN_DURATION = 0.8f;
    private static final float PHASE_RETURN_END =
            PHASE_SWAY_END + RETURN_DURATION;

    private static final float SETTLE_DURATION = 1.68f;
    private static final float MOTION_DURATION =
            PHASE_RETURN_END + SETTLE_DURATION;

    private static final int LAST_SWAY_SEGMENT =
            SWAY_CYCLE_COUNT * 2 - 1;

    // ============================================================
    // 동작 범위 — 조절 완료된 최종 고정값 (직접 수정)
    // ============================================================
    private static final float LOOK_LEFT_OFFSET = -6.2f;
    private static final float LOOK_RIGHT_OFFSET = 5.7f;
    private static final float HEAD_DOWN_OFFSET = -5.3f;
    private static final float HEAD_TILT_OFFSET = 6.2f;
    private static final float BODY_OFFSET = 7.8f;

    /** 얼굴 홍조 강도 (0~1). Happy 표정 약 0.30 기준으로 연하게 설정 */
    private static final float BLUSH_INTENSITY = 0.52f;

    private final CubismId idParamAngleX;
    private final CubismId idParamAngleY;
    private final CubismId idParamAngleZ;
    private final CubismId idParamBodyAngleX;
    private final CubismId idExpFaceBlush;

    private float elapsed = 0.0f;
    private boolean active = false;

    private float baseAngleX;
    private float baseAngleY;
    private float baseAngleZ;
    private float baseBodyAngleX;
    private float baseFaceBlush;

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

        if (elapsed < PHASE_HEAD_DOWN_END) {
            // 지루한 듯 고개를 살짝 숙임
            float t = smoothStep(elapsed / PHASE_HEAD_DOWN_END);
            offsetY = HEAD_DOWN_OFFSET * t;
            offsetZ = HEAD_TILT_OFFSET * t;

        } else if (elapsed < PHASE_SWAY_END) {
            // 좌우로 SWAY_CYCLE_COUNT번 흔듦
            float swayElapsed = elapsed - PHASE_HEAD_DOWN_END;
            int segmentIndex;
            float segmentT;

            if (swayElapsed >= SWAY_TOTAL_DURATION) {
                segmentIndex = LAST_SWAY_SEGMENT;
                segmentT = 1.0f;
            } else {
                segmentIndex = (int) (
                        swayElapsed / SWAY_HALF_SEGMENT_DURATION
                );
                float rawT = (
                        swayElapsed
                                - segmentIndex * SWAY_HALF_SEGMENT_DURATION
                ) / SWAY_HALF_SEGMENT_DURATION;
                segmentT = smoothStep(rawT);
            }

            offsetY = HEAD_DOWN_OFFSET;
            offsetX = swayOffsetX(segmentIndex, segmentT);
            offsetZ = swayOffsetZ(segmentIndex, segmentT);
            bodyOffsetX = swayBodyOffsetX(segmentIndex, segmentT);

        } else if (elapsed < PHASE_RETURN_END) {
            // sway 마지막 자세에서 정면으로 부드럽게 복귀
            float t = smoothStep(
                    (elapsed - PHASE_SWAY_END) / RETURN_DURATION
            );
            offsetX = lerp(
                    swayOffsetX(LAST_SWAY_SEGMENT, 1.0f),
                    0.0f,
                    t
            );
            offsetY = lerp(HEAD_DOWN_OFFSET, 0.0f, t);
            offsetZ = lerp(
                    swayOffsetZ(LAST_SWAY_SEGMENT, 1.0f),
                    0.0f,
                    t
            );
            bodyOffsetX = lerp(
                    swayBodyOffsetX(LAST_SWAY_SEGMENT, 1.0f),
                    0.0f,
                    t
            );

        } else {
            // 마지막 자세를 안정시키며 기본 자세 유지
            offsetX = 0.0f;
            offsetY = 0.0f;
            offsetZ = 0.0f;
            bodyOffsetX = 0.0f;
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
        model.setParameterValue(
                idExpFaceBlush,
                baseFaceBlush + computeBlushOffset()
        );

        if (elapsed >= MOTION_DURATION) {
            applyBasePose(model);
            active = false;
            return true;
        }

        return false;
    }

    private float computeBlushOffset() {
        if (elapsed < PHASE_HEAD_DOWN_END) {
            float t = smoothStep(elapsed / PHASE_HEAD_DOWN_END);
            return BLUSH_INTENSITY * t;
        }

        if (elapsed < PHASE_SWAY_END) {
            return BLUSH_INTENSITY;
        }

        if (elapsed < PHASE_RETURN_END) {
            float t = smoothStep(
                    (elapsed - PHASE_SWAY_END) / RETURN_DURATION
            );
            return lerp(BLUSH_INTENSITY, 0.0f, t);
        }

        return 0.0f;
    }

    private static float swayOffsetX(int segmentIndex, float segmentT) {
        if (segmentIndex == 0) {
            return lerp(0.0f, LOOK_LEFT_OFFSET, segmentT);
        }
        if (segmentIndex % 2 == 1) {
            return lerp(LOOK_LEFT_OFFSET, LOOK_RIGHT_OFFSET, segmentT);
        }
        return lerp(LOOK_RIGHT_OFFSET, LOOK_LEFT_OFFSET, segmentT);
    }

    private static float swayOffsetZ(int segmentIndex, float segmentT) {
        if (segmentIndex == 0) {
            return HEAD_TILT_OFFSET;
        }
        if (segmentIndex % 2 == 1) {
            return lerp(HEAD_TILT_OFFSET, -HEAD_TILT_OFFSET, segmentT);
        }
        return lerp(-HEAD_TILT_OFFSET, HEAD_TILT_OFFSET, segmentT);
    }

    private static float swayBodyOffsetX(int segmentIndex, float segmentT) {
        if (segmentIndex == 0) {
            return lerp(0.0f, BODY_OFFSET, segmentT);
        }
        if (segmentIndex % 2 == 1) {
            return lerp(BODY_OFFSET, 0.0f, segmentT);
        }
        return lerp(0.0f, BODY_OFFSET, segmentT);
    }

    private void applyBasePose(CubismModel model) {
        model.setParameterValue(idParamAngleX, baseAngleX);
        model.setParameterValue(idParamAngleY, baseAngleY);
        model.setParameterValue(idParamAngleZ, baseAngleZ);
        model.setParameterValue(idParamBodyAngleX, baseBodyAngleX);
        model.setParameterValue(idExpFaceBlush, baseFaceBlush);
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
