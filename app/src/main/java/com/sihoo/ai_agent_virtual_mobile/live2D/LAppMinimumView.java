/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.sihoo.ai_agent_virtual_mobile.live2D;

import com.sihoo.ai_agent_virtual_mobile.live2D.demo.LAppDefine;
import com.sihoo.ai_agent_virtual_mobile.live2D.demo.TouchManager;
import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.math.CubismViewMatrix;
import com.live2d.sdk.cubism.framework.rendering.android.CubismRenderTargetAndroid;
import android.os.SystemClock;
import android.util.Log;

import java.util.Locale;

public class LAppMinimumView implements AutoCloseable {
    /**
     * LAppMinimumModelのレンダリング先
     */

    private static final float HEAD_CENTER_X = 0.00f;
    private static final float HEAD_CENTER_Y = 0.75f;
    private static final float LOOK_RANGE_X = 0.67f;
    private static final float LOOK_RANGE_Y = 1.45f;
    private static final float HEAD_HIT_RADIUS_X = 0.15f;
    private static final float HEAD_HIT_RADIUS_Y = 0.15f;
    private static final float BODY_CENTER_X = 0.00f;
    private static final float BODY_CENTER_Y = 0.32f;
    private static final float BODY_HIT_RADIUS_X = 0.20f;
    private static final float BODY_HIT_RADIUS_Y = 0.35f;
    private static final float BODY_CHEST_BELLY_SPLIT_Y = 0.35f;
    private static final float DRAG_START_DISTANCE_PX = 24.0f;
    private static final long DOUBLE_TAP_MAX_INTERVAL_MS = 320L;
    private static final float DOUBLE_TAP_MAX_DISTANCE_PX = 40.0f;
    private static final long MOVE_LOG_INTERVAL_MS = 250L;
    private static final String TOUCH_LOG_TAG = "PetTouch";
    private static final boolean DEBUG_DRAW_HIT_AREAS = false;

    public enum HitRegion {
        NONE,
        HEAD,
        CHEST,
        BELLY
    } 

    public enum RenderingTarget {
        NONE,   // デフォルトのフレームバッファにレンダリング
        MODEL_FRAME_BUFFER,     // LAppMinimumModelが各自持つフレームバッファにレンダリング
        VIEW_FRAME_BUFFER  // LAppMinimumViewが持つフレームバッファにレンダリング
    }

    public LAppMinimumView() {
        clearColor[0] = 1.0f;
        clearColor[1] = 1.0f;
        clearColor[2] = 1.0f;
        clearColor[3] = 0.0f;
    }

    @Override
    public void close() {
        renderingBuffer.destroyRenderTarget();

        renderingSprite = null;

        if (spriteShader != null) {
            spriteShader.close();
            spriteShader = null;
        }

        if (hitOverlay != null) {
            hitOverlay.close();
            hitOverlay = null;
        }
    }

    // ビューを初期化する
    public void initialize() {
        int width = LAppMinimumDelegate.getInstance().getWindowWidth();
        int height = LAppMinimumDelegate.getInstance().getWindowHeight();

        float ratio = (float) width / (float) height;
        float left = -ratio;
        float right = ratio;
        float bottom = LAppDefine.LogicalView.LEFT.getValue();
        float top = LAppDefine.LogicalView.RIGHT.getValue();

        // デバイスに対応する画面範囲。Xの左端、Xの右端、Yの下端、Yの上端
        viewMatrix.setScreenRect(left, right, bottom, top);
        viewMatrix.scale(LAppDefine.Scale.DEFAULT.getValue(), LAppDefine.Scale.DEFAULT.getValue());

        // 単位行列に初期化
        deviceToScreen.loadIdentity();

        if (width > height) {
            float screenW = Math.abs(right - left);
            deviceToScreen.scaleRelative(screenW / width, -screenW / width);
        } else {
            float screenH = Math.abs(top - bottom);
            deviceToScreen.scaleRelative(screenH / height, -screenH / height);
        }
        deviceToScreen.translateRelative(-width * 0.5f, -height * 0.5f);

        // 表示範囲の設定
        viewMatrix.setMaxScale(LAppDefine.Scale.MAX.getValue());   // 限界拡大率
        viewMatrix.setMinScale(LAppDefine.Scale.MIN.getValue());   // 限界縮小率

        // 表示できる最大範囲
        viewMatrix.setMaxScreenRect(
            LAppDefine.LogicalView.LEFT.getValue(),
            LAppDefine.LogicalView.RIGHT.getValue(),
            LAppDefine.LogicalView.BOTTOM.getValue(),
            LAppDefine.MaxLogicalView.TOP.getValue()
        );

        spriteShader = new LAppMinimumSpriteShader();

        if (hitOverlay != null) {
            hitOverlay.close();
            hitOverlay = null;
        }
        if (DEBUG_DRAW_HIT_AREAS) {
            hitOverlay = new HitRegionDebugOverlay();
        }
    }

    // 画像を初期化する
    public void initializeSprite() {
        int windowWidth = LAppMinimumDelegate.getInstance().getWindowWidth();
        int windowHeight = LAppMinimumDelegate.getInstance().getWindowHeight();

        // 画面全体を覆うサイズ
        float x = windowWidth * 0.5f;
        float y = windowHeight * 0.5f;

        if (renderingSprite == null) {
            renderingSprite = new LAppMinimumSprite(x, y, windowWidth, windowHeight, 0, spriteShader.getShaderId());
        } else {
            renderingSprite.resize(x, y, windowWidth, windowHeight);
        }
    }

    // 描画する
    public void render() {
        // 画面サイズを取得する。
        int maxWidth = LAppMinimumDelegate.getInstance().getWindowWidth();
        int maxHeight = LAppMinimumDelegate.getInstance().getWindowHeight();

        // モデルの描画
        LAppMinimumLive2DManager.getInstance().onUpdate();

        // 各モデルが持つ描画ターゲットをテクスチャとする場合
        if (renderingTarget == RenderingTarget.MODEL_FRAME_BUFFER && renderingSprite != null) {
            final float[] uvVertex = {
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f
            };

            LAppMinimumModel model = LAppMinimumLive2DManager.getInstance().getModel(0);
            float alpha = getSpriteAlpha(2);    // 片方のみ不透明度を取得できるようにする。

            renderingSprite.setColor(1.0f, 1.0f, 1.0f, alpha);

            if (model != null) {
                renderingSprite.setWindowSize(maxWidth, maxHeight);
                renderingSprite.renderImmediate(model.getRenderingBuffer().getColorBuffer()[0], uvVertex);
            }
        }

        drawHitRegionOverlay();
    }

    /**
     * モデル1体を描画する直前にコールされる
     *
     * @param refModel モデルデータ
     */
    public void preModelDraw(LAppMinimumModel refModel) {
        // 別のレンダリングターゲットへ向けて描画する場合の使用するレンダーターゲット
        CubismRenderTargetAndroid useTarget;

        // 別のレンダリングターゲットへ向けて描画する場合
        if (renderingTarget != RenderingTarget.NONE) {

            // 使用するターゲット
            useTarget = (renderingTarget == RenderingTarget.VIEW_FRAME_BUFFER)
                ? renderingBuffer
                : refModel.getRenderingBuffer();

            // 描画ターゲット内部未作成の場合はここで作成
            if (!useTarget.isValid()) {
                int width = LAppMinimumDelegate.getInstance().getWindowWidth();
                int height = LAppMinimumDelegate.getInstance().getWindowHeight();

                // モデル描画キャンバス
                useTarget.createRenderTarget(width, height, null);
            }
            // レンダリング開始
            useTarget.beginDraw();
            useTarget.clear(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);   // 背景クリアカラー
        }
    }

    /**
     * モデル1体を描画した直後にコールされる
     *
     * @param refModel モデルデータ
     */
    public void postModelDraw(LAppMinimumModel refModel) {
        CubismRenderTargetAndroid useTarget = null;

        // 別のレンダリングターゲットへ向けて描画する場合
        if (renderingTarget != RenderingTarget.NONE) {
            // 使用するターゲット
            useTarget = (renderingTarget == RenderingTarget.VIEW_FRAME_BUFFER)
                ? renderingBuffer
                : refModel.getRenderingBuffer();

            // レンダリング終了
            useTarget.endDraw();

            // LAppViewの持つフレームバッファを使うなら、スプライトへの描画はこことなる
            if (renderingTarget == RenderingTarget.VIEW_FRAME_BUFFER && renderingSprite != null) {
                final float[] uvVertex = {
                    1.0f, 1.0f,
                    0.0f, 1.0f,
                    0.0f, 0.0f,
                    1.0f, 0.0f
                };
                renderingSprite.setColor(1.0f, 1.0f, 1.0f, getSpriteAlpha(0));

                // 画面サイズを取得する。
                int maxWidth = LAppMinimumDelegate.getInstance().getWindowWidth();
                int maxHeight = LAppMinimumDelegate.getInstance().getWindowHeight();

                renderingSprite.setWindowSize(maxWidth, maxHeight);
                renderingSprite.renderImmediate(useTarget.getColorBuffer()[0], uvVertex);
            }
        }
    }

    /**
     * レンダリング先を切り替える
     *
     * @param targetType レンダリング先
     */
    public void switchRenderingTarget(RenderingTarget targetType) {
        renderingTarget = targetType;
    }

    /**
     * タッチされたときに呼ばれる
     *
     * @param pointX スクリーンX座標
     * @param pointY スクリーンY座標
     */
    public void onTouchesBegan(float pointX, float pointY) {
        touchManager.touchesBegan(pointX, pointY);

        LAppMinimumLive2DManager manager =
                LAppMinimumLive2DManager.getInstance();
        ignoreHeadGestureThisTouch = manager.onUserActivity();

        float viewX = transformViewX(pointX);
        float viewY = transformViewY(pointY);
        float localX = toModelLocalX(viewX);
        float localY = toModelLocalY(viewY);
        HitRegion hitRegion = resolveHitRegion(localX, localY);
        boolean canStartGesture = !ignoreHeadGestureThisTouch
                && manager.canStartHeadInteraction();

        touchStartX = pointX;
        touchStartY = pointY;
        touchStartedRegion = canStartGesture ? hitRegion : HitRegion.NONE;
        isHeadPatting = false;
        isBodyStroking = false;
        lookStartedThisTouch = false;
        pinchStartedThisTouch = false;
        pinchWaitLoggedThisTouch = false;
        lastMoveLogMs = 0L;

        logTouch(
                "TOUCH_DOWN"
                        + " hit=" + hitRegion
                        + " gesture=" + touchStartedRegion
                        + " state=" + manager.getCurrentState()
                        + " blocked=" + ignoreHeadGestureThisTouch
                        + " canStart=" + manager.canStartHeadInteraction()
                        + " screen=(" + fmt(pointX) + ", " + fmt(pointY) + ")"
                        + modelSpaceLog(viewX, viewY, localX, localY)
                        + " splitY=" + fmt(BODY_CHEST_BELLY_SPLIT_Y)
                        + " yVsSplit=" + (localY >= BODY_CHEST_BELLY_SPLIT_Y
                        ? "above" : "below")
        );

        lastTouchViewX = viewX;
        lastTouchViewY = viewY;
        lastTouchHitRegion = hitRegion;
        hasLastTouch = true;
    }

    public void onTouchesMoved(float pointX, float pointY) {
        touchManager.touchesMoved(pointX, pointY);

        float viewX = transformViewX(pointX);
        float viewY = transformViewY(pointY);
        float localX = toModelLocalX(viewX);
        float localY = toModelLocalY(viewY);
        LAppMinimumLive2DManager manager =
                LAppMinimumLive2DManager.getInstance();
        HitRegion liveRegion = resolveHitRegion(localX, localY);
        lastTouchViewX = viewX;
        lastTouchViewY = viewY;
        lastTouchHitRegion = liveRegion;
        hasLastTouch = true;

        if (touchStartedRegion == HitRegion.HEAD) {
            float distance = distance(pointX, pointY, touchStartX, touchStartY);

            if (!isHeadPatting && distance >= DRAG_START_DISTANCE_PX) {
                isHeadPatting = true;
                logTouch(
                        "HEAD_DRAG_START"
                                + " distPx=" + fmt(distance)
                                + " liveHit=" + liveRegion
                                + modelSpaceLog(viewX, viewY, localX, localY)
                );
            }

            if (isHeadPatting) {
                float patX = toPatCoordinateX(localX);
                float patY = toPatCoordinateY(localY);
                logMoveThrottled(
                        "HEAD_PAT_MOVE"
                                + " pat=(" + fmt(patX) + ", " + fmt(patY) + ")"
                                + " liveHit=" + liveRegion
                                + modelSpaceLog(viewX, viewY, localX, localY)
                );
                manager.onHeadPat(patX, patY);
                return;
            }

            logMoveThrottled(
                    "HEAD_HOLD"
                            + " distPx=" + fmt(distance)
                            + " liveHit=" + liveRegion
                            + modelSpaceLog(viewX, viewY, localX, localY)
            );
            return;
        }

        if (isBodyRegion(touchStartedRegion)) {
            float distance = distance(pointX, pointY, touchStartX, touchStartY);

            if (!isBodyStroking && distance >= DRAG_START_DISTANCE_PX) {
                isBodyStroking = true;
                logTouch(
                        "BODY_DRAG_START"
                                + " region=" + touchStartedRegion
                                + " distPx=" + fmt(distance)
                                + " liveHit=" + liveRegion
                                + modelSpaceLog(viewX, viewY, localX, localY)
                );
            }

            if (isBodyStroking) {
                float strokeX = toBodyStrokeX(localX);
                float strokeY = toBodyStrokeY(localY);
                logMoveThrottled(
                        "BODY_STROKE_MOVE"
                                + " region=" + touchStartedRegion
                                + " stroke=(" + fmt(strokeX) + ", " + fmt(strokeY) + ")"
                                + " liveHit=" + liveRegion
                                + modelSpaceLog(viewX, viewY, localX, localY)
                );
                manager.onBodyStroke(strokeX, strokeY, touchStartedRegion);
                return;
            }

            logMoveThrottled(
                    "BODY_HOLD"
                            + " region=" + touchStartedRegion
                            + " distPx=" + fmt(distance)
                            + " liveHit=" + liveRegion
                            + modelSpaceLog(viewX, viewY, localX, localY)
            );
            return;
        }

        float relativeX = localX - HEAD_CENTER_X;
        float relativeY = localY - HEAD_CENTER_Y;

        float lookX = clamp(relativeX / LOOK_RANGE_X, -1.0f, 1.0f);
        float lookY = clamp(relativeY / LOOK_RANGE_Y, -1.0f, 1.0f);

        if (!lookStartedThisTouch) {
            lookStartedThisTouch = true;
            logTouch(
                    "LOOK_START"
                            + " liveHit=" + liveRegion
                            + " look=(" + fmt(lookX) + ", " + fmt(lookY) + ")"
                            + modelSpaceLog(viewX, viewY, localX, localY)
            );
        } else {
            logMoveThrottled(
                    "LOOK_MOVE"
                            + " liveHit=" + liveRegion
                            + " look=(" + fmt(lookX) + ", " + fmt(lookY) + ")"
                            + modelSpaceLog(viewX, viewY, localX, localY)
            );
        }

        manager.onDrag(lookX, lookY);
    }

    /**
     * タッチしているときにポインターが動いたら呼ばれる
     *
     * @param pointX スクリーンX座標
     * @param pointY スクリーンY座標
     */

    public void onTouchesMoved(
            float x1,
            float y1,
            float x2,
            float y2
    ) {
        float previousDeviceCenterX = touchManager.getLastX();
        float previousDeviceCenterY = touchManager.getLastY();

        float currentDeviceCenterX = (x1 + x2) * 0.5f;
        float currentDeviceCenterY = (y1 + y2) * 0.5f;

        boolean hasPreviousPinch =
                touchManager.getLastTouchDistance() > 0.0f;

        if (isHeadPatting) {
            logTouch("PINCH_CANCEL_HEAD_PAT");
            LAppMinimumLive2DManager.getInstance().cancelHeadPat();
            isHeadPatting = false;
            touchStartedRegion = HitRegion.NONE;
        }

        if (isBodyStroking) {
            logTouch("PINCH_CANCEL_BODY_STROKE region=" + touchStartedRegion);
            LAppMinimumLive2DManager.getInstance().cancelBodyStroke();
            isBodyStroking = false;
            touchStartedRegion = HitRegion.NONE;
        }

        if (touchStartedRegion != HitRegion.NONE) {
            logTouch("PINCH_CANCEL_GESTURE region=" + touchStartedRegion);
            touchStartedRegion = HitRegion.NONE;
        }

        touchManager.touchesMoved(x1, y1, x2, y2);

        if (!hasPreviousPinch) {
            if (!pinchWaitLoggedThisTouch) {
                pinchWaitLoggedThisTouch = true;
                logTouch(
                        "PINCH_WAIT_SECOND_MOVE"
                                + " p1=(" + fmt(x1) + ", " + fmt(y1) + ")"
                                + " p2=(" + fmt(x2) + ", " + fmt(y2) + ")"
                );
            }
            return;
        }

        float previousCenterX =
                transformViewX(previousDeviceCenterX);

        float previousCenterY =
                transformViewY(previousDeviceCenterY);

        float currentCenterX =
                transformViewX(currentDeviceCenterX);

        float currentCenterY =
                transformViewY(currentDeviceCenterY);

        if (!pinchStartedThisTouch) {
            pinchStartedThisTouch = true;
            logTouch(
                    "PINCH_START"
                            + " scale=" + fmt(touchManager.getScale())
                            + " previousCenter=("
                            + fmt(previousCenterX)
                            + ", "
                            + fmt(previousCenterY)
                            + ")"
                            + " currentCenter=("
                            + fmt(currentCenterX)
                            + ", "
                            + fmt(currentCenterY)
                            + ")"
            );
        } else {
            logMoveThrottled(
                    "PINCH_MOVE"
                            + " scale=" + fmt(touchManager.getScale())
                            + " currentCenter=("
                            + fmt(currentCenterX)
                            + ", "
                            + fmt(currentCenterY)
                            + ")"
            );
        }

        LAppMinimumLive2DManager.getInstance().onPinch(
                touchManager.getScale(),
                previousCenterX,
                previousCenterY,
                currentCenterX,
                currentCenterY
        );
    }




    /**
     * タッチが終了したら呼ばれる
     *
     * @param pointX スクリーンX座標
     * @param pointY スクリーンY座標
     */
    public void onTouchesEnded(float pointX, float pointY) {
        LAppMinimumLive2DManager manager =
                LAppMinimumLive2DManager.getInstance();
        manager.onDrag(0.0f, 0.0f);

        float viewX = transformViewX(pointX);
        float viewY = transformViewY(pointY);
        float localX = toModelLocalX(viewX);
        float localY = toModelLocalY(viewY);
        HitRegion liveRegion = resolveHitRegion(localX, localY);
        float moveDistance = distance(pointX, pointY, touchStartX, touchStartY);

        lastTouchViewX = viewX;
        lastTouchViewY = viewY;
        lastTouchHitRegion = liveRegion;
        hasLastTouch = true;

        if (isHeadPatting) {
            logTouch(
                    "HEAD_PAT_END"
                            + " distPx=" + fmt(moveDistance)
                            + " liveHit=" + liveRegion
                            + modelSpaceLog(viewX, viewY, localX, localY)
            );
            manager.onHeadPatEnd();
            isHeadPatting = false;
            touchStartedRegion = HitRegion.NONE;
            lastTapRegion = HitRegion.NONE;
            resetTouchSession();
            return;
        }

        if (isBodyStroking) {
            logTouch(
                    "BODY_STROKE_END"
                            + " region=" + touchStartedRegion
                            + " distPx=" + fmt(moveDistance)
                            + " liveHit=" + liveRegion
                            + modelSpaceLog(viewX, viewY, localX, localY)
            );
            manager.onBodyStrokeEnd();
            isBodyStroking = false;
            touchStartedRegion = HitRegion.NONE;
            lastTapRegion = HitRegion.NONE;
            resetTouchSession();
            return;
        }

        boolean shortHeadTap = touchStartedRegion == HitRegion.HEAD
                && moveDistance < DRAG_START_DISTANCE_PX;
        boolean shortBodyTap = isBodyRegion(touchStartedRegion)
                && moveDistance < DRAG_START_DISTANCE_PX;

        long now = SystemClock.uptimeMillis();

        if (shortHeadTap) {
            boolean isDoubleTap = lastTapRegion == HitRegion.HEAD
                    && (now - lastTapTimeMs) <= DOUBLE_TAP_MAX_INTERVAL_MS
                    && distance(pointX, pointY, lastTapX, lastTapY)
                    <= DOUBLE_TAP_MAX_DISTANCE_PX;

            if (isDoubleTap) {
                logTouch(
                        "HEAD_DOUBLE_TAP"
                                + " intervalMs=" + (now - lastTapTimeMs)
                                + " liveHit=" + liveRegion
                                + modelSpaceLog(viewX, viewY, localX, localY)
                );
                manager.onHeadDoubleTap();
                lastTapRegion = HitRegion.NONE;
            } else {
                logTouch(
                        "HEAD_TAP"
                                + " waitingSecondTap=true"
                                + " liveHit=" + liveRegion
                                + modelSpaceLog(viewX, viewY, localX, localY)
                );
                lastTapTimeMs = now;
                lastTapX = pointX;
                lastTapY = pointY;
                lastTapRegion = HitRegion.HEAD;
            }
        } else if (shortBodyTap) {
            boolean isDoubleTap = lastTapRegion == touchStartedRegion
                    && (now - lastTapTimeMs) <= DOUBLE_TAP_MAX_INTERVAL_MS
                    && distance(pointX, pointY, lastTapX, lastTapY)
                    <= DOUBLE_TAP_MAX_DISTANCE_PX;

            if (isDoubleTap) {
                logTouch(
                        "BODY_DOUBLE_TAP"
                                + " region=" + touchStartedRegion
                                + " intervalMs=" + (now - lastTapTimeMs)
                                + " liveHit=" + liveRegion
                                + modelSpaceLog(viewX, viewY, localX, localY)
                );
                manager.onBodyDoubleTap(touchStartedRegion);
                lastTapRegion = HitRegion.NONE;
            } else {
                logTouch(
                        "BODY_TAP"
                                + " region=" + touchStartedRegion
                                + " lastTap=" + lastTapRegion
                                + " waitingSecondTap=true"
                                + " liveHit=" + liveRegion
                                + modelSpaceLog(viewX, viewY, localX, localY)
                );
                lastTapTimeMs = now;
                lastTapX = pointX;
                lastTapY = pointY;
                lastTapRegion = touchStartedRegion;
            }
        } else if (pinchStartedThisTouch) {
            logTouch(
                    "PINCH_END"
                            + " liveHit=" + liveRegion
                            + modelSpaceLog(viewX, viewY, localX, localY)
            );
            lastTapRegion = HitRegion.NONE;
        } else if (lookStartedThisTouch) {
            logTouch(
                    "LOOK_END"
                            + " liveHit=" + liveRegion
                            + modelSpaceLog(viewX, viewY, localX, localY)
            );
            lastTapRegion = HitRegion.NONE;
        } else {
            logTouch(
                    "TOUCH_UP"
                            + " started=" + touchStartedRegion
                            + " distPx=" + fmt(moveDistance)
                            + " liveHit=" + liveRegion
                            + " blocked=" + ignoreHeadGestureThisTouch
                            + modelSpaceLog(viewX, viewY, localX, localY)
            );
            lastTapRegion = HitRegion.NONE;
        }

        resetTouchSession();
    }

    private void resetTouchSession() {
        touchStartedRegion = HitRegion.NONE;
        ignoreHeadGestureThisTouch = false;
        lookStartedThisTouch = false;
        pinchStartedThisTouch = false;
        pinchWaitLoggedThisTouch = false;
        lastMoveLogMs = 0L;
    }

    private HitRegion resolveHitRegion(float localX, float localY) {
        if (isInsideHead(localX, localY)) {
            return HitRegion.HEAD;
        }
        if (isInsideBody(localX, localY)) {
            return localY >= BODY_CHEST_BELLY_SPLIT_Y
                    ? HitRegion.CHEST
                    : HitRegion.BELLY;
        }
        return HitRegion.NONE;
    }

    private float toModelLocalX(float viewX) {
        LAppMinimumLive2DManager manager =
                LAppMinimumLive2DManager.getInstance();
        float scale = manager.getUserScale();
        if (scale < 0.0001f) {
            scale = 1.0f;
        }
        return (viewX - manager.getUserOffsetX()) / scale;
    }

    private float toModelLocalY(float viewY) {
        LAppMinimumLive2DManager manager =
                LAppMinimumLive2DManager.getInstance();
        float scale = manager.getUserScale();
        if (scale < 0.0001f) {
            scale = 1.0f;
        }
        return (viewY - manager.getUserOffsetY()) / scale;
    }

    private String modelSpaceLog(
            float viewX,
            float viewY,
            float localX,
            float localY
    ) {
        LAppMinimumLive2DManager manager =
                LAppMinimumLive2DManager.getInstance();
        return " view=(" + fmt(viewX) + ", " + fmt(viewY) + ")"
                + " local=(" + fmt(localX) + ", " + fmt(localY) + ")"
                + " scale=" + fmt(manager.getUserScale())
                + " offset=("
                + fmt(manager.getUserOffsetX())
                + ", "
                + fmt(manager.getUserOffsetY())
                + ")";
    }

    private static boolean isBodyRegion(HitRegion region) {
        return region == HitRegion.CHEST || region == HitRegion.BELLY;
    }

    private void logTouch(String message) {
        Log.d(TOUCH_LOG_TAG, message);
    }

    private void logMoveThrottled(String message) {
        long now = SystemClock.uptimeMillis();
        if (lastMoveLogMs != 0L && now - lastMoveLogMs < MOVE_LOG_INTERVAL_MS) {
            return;
        }
        lastMoveLogMs = now;
        logTouch(message);
    }

    private static String fmt(float value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private boolean isInsideBody(float viewX, float viewY) {
        float dx = (viewX - BODY_CENTER_X) / BODY_HIT_RADIUS_X;
        float dy = (viewY - BODY_CENTER_Y) / BODY_HIT_RADIUS_Y;
        return (dx * dx) + (dy * dy) <= 1.0f;
    }

    private float toBodyStrokeX(float viewX) {
        return clamp(
                (viewX - BODY_CENTER_X) / BODY_HIT_RADIUS_X,
                -1.0f,
                1.0f
        );
    }

    private float toBodyStrokeY(float viewY) {
        return clamp(
                (viewY - BODY_CENTER_Y) / BODY_HIT_RADIUS_Y,
                -1.0f,
                1.0f
        );
    }

    private boolean isInsideHead(float viewX, float viewY) {
        float dx = (viewX - HEAD_CENTER_X) / HEAD_HIT_RADIUS_X;
        float dy = (viewY - HEAD_CENTER_Y) / HEAD_HIT_RADIUS_Y;
        return (dx * dx) + (dy * dy) <= 1.0f;
    }

    private float toPatCoordinateX(float viewX) {
        return clamp(
                (viewX - HEAD_CENTER_X) / HEAD_HIT_RADIUS_X,
                -1.0f,
                1.0f
        );
    }

    private float toPatCoordinateY(float viewY) {
        return clamp(
                (viewY - HEAD_CENTER_Y) / HEAD_HIT_RADIUS_Y,
                -1.0f,
                1.0f
        );
    }

    private static float distance(
            float x1,
            float y1,
            float x2,
            float y2
    ) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Convert hit-test view coordinates into OpenGL clip space.
     */
    public void viewToNdc(float viewX, float viewY, float[] outXy) {
        int width = LAppMinimumDelegate.getInstance().getWindowWidth();
        int height = LAppMinimumDelegate.getInstance().getWindowHeight();
        if (width <= 0 || height <= 0 || outXy == null || outXy.length < 2) {
            return;
        }

        float screenX = viewMatrix.transformX(viewX);
        float screenY = viewMatrix.transformY(viewY);
        float deviceX = deviceToScreen.invertTransformX(screenX);
        float deviceY = deviceToScreen.invertTransformY(screenY);

        outXy[0] = (deviceX / (float) width) * 2.0f - 1.0f;
        outXy[1] = 1.0f - (deviceY / (float) height) * 2.0f;
    }

    private void drawHitRegionOverlay() {
        if (!DEBUG_DRAW_HIT_AREAS || hitOverlay == null) {
            return;
        }

        LAppMinimumLive2DManager manager =
                LAppMinimumLive2DManager.getInstance();
        float scale = manager.getUserScale();
        if (scale < 0.0001f) {
            scale = 1.0f;
        }
        float offsetX = manager.getUserOffsetX();
        float offsetY = manager.getUserOffsetY();

        hitOverlay.draw(
                this,
                HEAD_CENTER_X * scale + offsetX,
                HEAD_CENTER_Y * scale + offsetY,
                HEAD_HIT_RADIUS_X * scale,
                HEAD_HIT_RADIUS_Y * scale,
                BODY_CENTER_X * scale + offsetX,
                BODY_CENTER_Y * scale + offsetY,
                BODY_HIT_RADIUS_X * scale,
                BODY_HIT_RADIUS_Y * scale,
                BODY_CHEST_BELLY_SPLIT_Y * scale + offsetY,
                lastTouchViewX,
                lastTouchViewY,
                hasLastTouch,
                lastTouchHitRegion
        );
    }
    public float transformViewX(float deviceX) {
        // 論理座標変換した座標を取得
        float screenX = deviceToScreen.transformX(deviceX);
        // 拡大、縮小、移動後の値
        return viewMatrix.invertTransformX(screenX);
    }

    /**
     * Y座標をView座標に変換する
     *
     * @param deviceY デバイスY座標
     * @return ViewY座標
     */
    public float transformViewY(float deviceY) {
        // 論理座標変換した座標を取得
        float screenY = deviceToScreen.transformY(deviceY);
        // 拡大、縮小、移動後の値
        return viewMatrix.invertTransformY(screenY);
    }

    /**
     * レンダリング先をデフォルト以外に切り替えた際の背景クリア色設定
     *
     * @param r 赤(0.0~1.0)
     * @param g 緑(0.0~1.0)
     * @param b 青(0.0~1.0)
     */
    public void setRenderingTargetClearColor(float r, float g, float b) {
        clearColor[0] = r;
        clearColor[1] = g;
        clearColor[2] = b;
    }

    /**
     * 別レンダリングターゲットにモデルを描画するサンプルで描画時のαを決定する
     *
     * @param assign α値の算出に使用する値
     * @return 算出されたα値
     */
    public float getSpriteAlpha(int assign) {
        // assignの数値に応じて適当な差をつける
        float alpha = 0.25f + (float) assign * 0.5f;

        // サンプルとしてαに適当な差をつける
        if (alpha > 1.0f) {
            alpha = 1.0f;
        }
        if (alpha < 0.1f) {
            alpha = 0.1f;
        }
        return alpha;
    }

    /**
     * Return rendering target enum instance.
     *
     * @return rendering target
     */
    public RenderingTarget getRenderingTarget() {
        return renderingTarget;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private final CubismMatrix44 deviceToScreen = CubismMatrix44.create(); // デバイス座標からスクリーン座標に変換するための行列
    private final CubismViewMatrix viewMatrix = new CubismViewMatrix();   // 画面表示の拡縮や移動の変換を行う行列

    /**
     * レンダリング先の選択肢
     */
    private RenderingTarget renderingTarget = RenderingTarget.NONE;

    /**
     * レンダリングターゲットのクリアカラー
     */
    private final float[] clearColor = new float[4];

    private CubismRenderTargetAndroid renderingBuffer = new CubismRenderTargetAndroid();

    private LAppMinimumSprite renderingSprite;

    private final TouchManager touchManager = new TouchManager();

    private float touchStartX;
    private float touchStartY;
    private HitRegion touchStartedRegion = HitRegion.NONE;
    private boolean isHeadPatting;
    private boolean isBodyStroking;
    private boolean ignoreHeadGestureThisTouch;
    private boolean lookStartedThisTouch;
    private boolean pinchStartedThisTouch;
    private boolean pinchWaitLoggedThisTouch;
    private long lastMoveLogMs;
    private long lastTapTimeMs;
    private float lastTapX;
    private float lastTapY;
    private HitRegion lastTapRegion = HitRegion.NONE;
    private HitRegionDebugOverlay hitOverlay;
    private float lastTouchViewX;
    private float lastTouchViewY;
    private boolean hasLastTouch;
    private HitRegion lastTouchHitRegion = HitRegion.NONE;

    /**
     * シェーダー作成委譲クラス
     */
    private LAppMinimumSpriteShader spriteShader;
}
