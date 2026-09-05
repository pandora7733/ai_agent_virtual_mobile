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

public class LAppMinimumView implements AutoCloseable {
    /**
     * LAppMinimumModelのレンダリング先
     */

    private static final float HEAD_CENTER_X = 0.00f;
    private static final float HEAD_CENTER_Y = 0.70f;
    private static final float LOOK_RANGE_X = 0.67f;
    private static final float LOOK_RANGE_Y = 1.45f;
    private static final float HEAD_HIT_RADIUS_X = 0.38f;
    private static final float HEAD_HIT_RADIUS_Y = 0.45f;
    private static final float BODY_CENTER_X = 0.00f;
    private static final float BODY_CENTER_Y = -0.05f;
    private static final float BODY_HIT_RADIUS_X = 0.42f;
    private static final float BODY_HIT_RADIUS_Y = 0.55f;
    private static final float DRAG_START_DISTANCE_PX = 24.0f;
    private static final long DOUBLE_TAP_MAX_INTERVAL_MS = 320L;
    private static final float DOUBLE_TAP_MAX_DISTANCE_PX = 40.0f; 

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
        boolean insideHead = isInsideHead(viewX, viewY);
        boolean insideBody = !insideHead && isInsideBody(viewX, viewY);

        touchStartX = pointX;
        touchStartY = pointY;
        touchStartedOnHead = !ignoreHeadGestureThisTouch
                && manager.canStartHeadInteraction()
                && insideHead;
        touchStartedOnBody = !ignoreHeadGestureThisTouch
                && manager.canStartBodyInteraction()
                && insideBody;
        isHeadPatting = false;
        isBodyStroking = false;

        LAppMinimumPal.printLog(
                "[APP] HEAD_HIT=" + insideHead
                        + ", BODY_HIT=" + insideBody
                        + ", startHeadGesture=" + touchStartedOnHead
                        + ", startBodyGesture=" + touchStartedOnBody
        );
    }

    public void onTouchesMoved(float pointX, float pointY) {
        touchManager.touchesMoved(pointX, pointY);

        float viewX = transformViewX(pointX);
        float viewY = transformViewY(pointY);
        LAppMinimumLive2DManager manager =
                LAppMinimumLive2DManager.getInstance();

        if (touchStartedOnHead) {
            float dx = pointX - touchStartX;
            float dy = pointY - touchStartY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (!isHeadPatting && distance >= DRAG_START_DISTANCE_PX) {
                isHeadPatting = true;
            }

            if (isHeadPatting) {
                manager.onHeadPat(
                        toPatCoordinateX(viewX),
                        toPatCoordinateY(viewY)
                );
                return;
            }

            return;
        }

        if (touchStartedOnBody) {
            float dx = pointX - touchStartX;
            float dy = pointY - touchStartY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (!isBodyStroking && distance >= DRAG_START_DISTANCE_PX) {
                isBodyStroking = true;
            }

            if (isBodyStroking) {
                manager.onBodyStroke(
                        toBodyStrokeX(viewX),
                        toBodyStrokeY(viewY)
                );
                return;
            }

            return;
        }

        float relativeX = viewX - HEAD_CENTER_X;
        float relativeY = viewY - HEAD_CENTER_Y;

        float lookX = clamp(relativeX / LOOK_RANGE_X, -1.0f, 1.0f);
        float lookY = clamp(relativeY / LOOK_RANGE_Y, -1.0f, 1.0f);

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
            LAppMinimumLive2DManager.getInstance().cancelHeadPat();
            isHeadPatting = false;
            touchStartedOnHead = false;
        }

        if (isBodyStroking) {
            LAppMinimumLive2DManager.getInstance().cancelBodyStroke();
            isBodyStroking = false;
            touchStartedOnBody = false;
        }

        touchManager.touchesMoved(x1, y1, x2, y2);

        if (!hasPreviousPinch) {
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

        LAppMinimumLive2DManager.getInstance().onPinch(
                touchManager.getScale(),
                previousCenterX,
                previousCenterY,
                currentCenterX,
                currentCenterY
        );

        Log.d(
                "PINCH",
                "scale=" + touchManager.getScale()
                        + ", previousCenter=("
                        + previousCenterX
                        + ", "
                        + previousCenterY
                        + ")"
                        + ", currentCenter=("
                        + currentCenterX
                        + ", "
                        + currentCenterY
                        + ")"
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

        if (isHeadPatting) {
            manager.onHeadPatEnd();
            isHeadPatting = false;
            touchStartedOnHead = false;
            lastTapWasOnHead = false;
            lastTapWasOnBody = false;
            return;
        }

        if (isBodyStroking) {
            manager.onBodyStrokeEnd();
            isBodyStroking = false;
            touchStartedOnBody = false;
            lastTapWasOnHead = false;
            lastTapWasOnBody = false;
            return;
        }

        float dx = pointX - touchStartX;
        float dy = pointY - touchStartY;
        float moveDistance = (float) Math.sqrt(dx * dx + dy * dy);
        boolean shortHeadTap = touchStartedOnHead
                && moveDistance < DRAG_START_DISTANCE_PX;
        boolean shortBodyTap = touchStartedOnBody
                && moveDistance < DRAG_START_DISTANCE_PX;

        long now = SystemClock.uptimeMillis();

        if (shortHeadTap) {
            boolean isDoubleTap = lastTapWasOnHead
                    && (now - lastTapTimeMs) <= DOUBLE_TAP_MAX_INTERVAL_MS
                    && distance(pointX, pointY, lastTapX, lastTapY)
                    <= DOUBLE_TAP_MAX_DISTANCE_PX;

            if (isDoubleTap) {
                manager.onHeadDoubleTap();
                lastTapWasOnHead = false;
                lastTapWasOnBody = false;
            } else {
                lastTapTimeMs = now;
                lastTapX = pointX;
                lastTapY = pointY;
                lastTapWasOnHead = true;
                lastTapWasOnBody = false;
            }
        } else if (shortBodyTap) {
            boolean isDoubleTap = lastTapWasOnBody
                    && (now - lastTapTimeMs) <= DOUBLE_TAP_MAX_INTERVAL_MS
                    && distance(pointX, pointY, lastTapX, lastTapY)
                    <= DOUBLE_TAP_MAX_DISTANCE_PX;

            if (isDoubleTap) {
                manager.onBodyDoubleTap();
                lastTapWasOnHead = false;
                lastTapWasOnBody = false;
            } else {
                lastTapTimeMs = now;
                lastTapX = pointX;
                lastTapY = pointY;
                lastTapWasOnHead = false;
                lastTapWasOnBody = true;
            }
        } else {
            lastTapWasOnHead = false;
            lastTapWasOnBody = false;
        }

        touchStartedOnHead = false;
        touchStartedOnBody = false;
        ignoreHeadGestureThisTouch = false;
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
     * X座標をView座標に変換する
     *
     * @param deviceX デバイスX座標
     * @return ViewX座標
     */
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
    private boolean touchStartedOnHead;
    private boolean touchStartedOnBody;
    private boolean isHeadPatting;
    private boolean isBodyStroking;
    private boolean ignoreHeadGestureThisTouch;
    private long lastTapTimeMs;
    private float lastTapX;
    private float lastTapY;
    private boolean lastTapWasOnHead;
    private boolean lastTapWasOnBody;

    /**
     * シェーダー作成委譲クラス
     */
    private LAppMinimumSpriteShader spriteShader;
}
