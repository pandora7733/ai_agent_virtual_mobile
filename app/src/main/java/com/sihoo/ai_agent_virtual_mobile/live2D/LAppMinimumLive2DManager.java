/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.sihoo.ai_agent_virtual_mobile.live2D;

import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.rendering.android.CubismOffscreenManagerAndroid;

/**
 * サンプルアプリケーションにおいてCubismModelを管理するクラス。
 * モデル生成と破棄、タップイベントの処理、モデル切り替えを行う。
 */
public class LAppMinimumLive2DManager {
    public static LAppMinimumLive2DManager getInstance() {
        if (s_instance == null) {
            s_instance = new LAppMinimumLive2DManager();
        }
        return s_instance;
    }

    public static void releaseInstance() {
        if (s_instance != null) {
            if (s_instance.model != null) {
                s_instance.model.deleteModel();
                s_instance.model = null;
            }
            CubismOffscreenManagerAndroid.releaseInstance();
        }
        s_instance = null;
    }

    public enum OutfitType{
        DEFAULT,
        OUTFIT,
        JACKET_OFF
    }

    public boolean applyOutfit(OutfitType outfitType) {
        if (model == null || outfitType == null) {
            return false;
        }

        switch (outfitType) {
            case OUTFIT:
                return model.setExpression("Outfit");

            case JACKET_OFF:
                return model.setExpression("JaketOFF");

            case DEFAULT:
                return model.clearExpression();

            default:
                return false;
        }
    }

    public void loadModel(String modelDirectoryName) {
        String dir = modelDirectoryName + "/";
        model = new LAppMinimumModel(dir);
        model.loadAssets(dir, modelDirectoryName + ".model3.json");

        petPreferences = new PetPreferences(
                LAppMinimumDelegate.getInstance().getActivity()
        );

        if (petPreferences.isFirstVisit()) {
            currentState = CharacterState.FIRST_VISIT;
            model.setIdleEffectsEnabled(false);
            model.startFirstVisitMotion();
        } else {
            currentState = CharacterState.IDLE;
            model.setIdleEffectsEnabled(true);
        }
    }

    // モデル更新処理及び描画処理を行う
    public void onUpdate() {
        int width = LAppMinimumDelegate.getInstance().getWindowWidth();
        int height = LAppMinimumDelegate.getInstance().getWindowHeight();
        float aspectRatio = (float) width / (float) height;
        float displayRatio = (float) height / (float) width;

        // モデルで使用するオフスクリーン管理の開始処理
        CubismOffscreenManagerAndroid.getInstance().beginFrameProcess();

        projection.loadIdentity();

        float canvasRatio = model.getModel().getCanvasHeight() / model.getModel().getCanvasWidth();

//        if (canvasRatio < displayRatio) {
//            // 横長モデルを幅に合わせて縦方向のスケールを調整
//            model.getModelMatrix().setWidth(2.0f);
//            projection.scale(1.0f, aspectRatio);
//        } else {
//            // 縦長モデルを高さに合わせて横方向のスケールを調整
//            model.getModelMatrix().setHeight(2.0f);
//            projection.scale(1.0f / aspectRatio, 1.0f);
//        }

        final float finalModelSize = 2.0f * userScale;

        if (canvasRatio < displayRatio) {
            model.getModelMatrix().setWidth(finalModelSize);
            projection.scale(1.0f, aspectRatio);
        } else {
            model.getModelMatrix().setHeight(finalModelSize);
            projection.scale(1.0f / aspectRatio, 1.0f);
        }

        model.getModelMatrix().setPosition(userOffsetX, userOffsetY);

        if (currentState == CharacterState.FIRST_VISIT
                && petPreferences != null
                && model.isFirstVisitMotionFinished()) {
            model.clearExpression();

            petPreferences.markFirstVisitCompleted();

            currentState = CharacterState.IDLE;
            model.setIdleEffectsEnabled(true);

            LAppMinimumPal.printLog(
                    "[APP] state changed: FIRST_VISIT -> IDLE"
            );
        }

        updateInactivityTimer();



        // 必要があればここで乗算する
        if (viewMatrix != null) {
            viewMatrix.multiplyByMatrix(projection);
        }

        // 描画前コール
        LAppMinimumDelegate.getInstance().getView().preModelDraw(model);

        model.update();
        model.draw(projection);     // 参照渡しなのでprojectionは変質する

        // 描画後コール
        LAppMinimumDelegate.getInstance().getView().postModelDraw(model);

        // モデルで使用するオフスクリーン管理の終了処理
        CubismOffscreenManagerAndroid.getInstance().endFrameProcess();
        // もし余っているオフスクリーンのリソースを解放したい場合行う処理
        CubismOffscreenManagerAndroid.getInstance().releaseStaleRenderTextures();
    }

    private void updateInactivityTimer() {
        if (petPreferences == null || model == null) {
            return;
        }

        float deltaTime = Math.max(
                0.0f,
                Math.min(
                        LAppMinimumPal.getDeltaTime(),
                        0.1f
                )
        );

        switch (currentState) {
            case IDLE:
                inactivityElapsedSeconds += deltaTime;

                if (inactivityElapsedSeconds >= SLEEP_AFTER_SECONDS) {
                    enterSleep("IDLE");
                } else if (!boredPlayedForCurrentInactivity
                        && inactivityElapsedSeconds >= BORED_AFTER_SECONDS) {
                    if (model.startBoredMotion()) {
                        boredPlayedForCurrentInactivity = true;
                        currentState = CharacterState.BORED;

                        LAppMinimumPal.printLog(
                                "[APP] state changed: IDLE -> BORED"
                        );
                    }
                }
                break;

            case BORED:
                inactivityElapsedSeconds += deltaTime;

                if (model.isBoredMotionFinished()) {
                    if (inactivityElapsedSeconds >= SLEEP_AFTER_SECONDS) {
                        enterSleep("BORED");
                    } else {
                        currentState = CharacterState.IDLE;

                        LAppMinimumPal.printLog(
                                "[APP] state changed: BORED -> IDLE"
                        );
                    }
                }
                break;

            case HEAD_PAT:
            case HEAD_DOUBLE_TAP:
                if (currentState == CharacterState.HEAD_PAT
                        && model.isHeadPatFinished()) {
                    currentState = CharacterState.IDLE;
                    inactivityElapsedSeconds = 0.0f;

                    LAppMinimumPal.printLog(
                            "[APP] state changed: HEAD_PAT -> IDLE"
                    );
                } else if (currentState == CharacterState.HEAD_DOUBLE_TAP
                        && model.isHeadDoubleTapFinished()) {
                    currentState = CharacterState.IDLE;
                    inactivityElapsedSeconds = 0.0f;

                    LAppMinimumPal.printLog(
                            "[APP] state changed: HEAD_DOUBLE_TAP -> IDLE"
                    );
                }
                break;

            case SLEEP_ENTRY:
                if (model.isSleepEntryFinished()) {
                    if (model.startSleepLoopMotion()) {
                        currentState = CharacterState.SLEEP;
                        sleepElapsedSeconds = 0.0f;

                        LAppMinimumPal.printLog(
                                "[APP] state changed: SLEEP_ENTRY -> SLEEP"
                        );
                    }
                }
                break;

            case SLEEP:
                sleepElapsedSeconds += deltaTime;

                if (sleepElapsedSeconds >= SLEEP_DURATION_SECONDS) {
                    wakeUpFromSleep("timeout");
                }
                break;

            case WAKING:
                if (model.isWakeMotionFinished()) {
                    finishWake();
                }
                break;

            default:
                break;
        }
    }

    private void enterSleep(String previousStateName) {
        if (!model.startSleepEntryMotion()) {
            return;
        }

        currentState = CharacterState.SLEEP_ENTRY;
        model.setIdleEffectsEnabled(false);

        LAppMinimumPal.printLog(
                "[APP] state changed: "
                        + previousStateName
                        + " -> SLEEP_ENTRY"
        );
    }

    private void wakeUpFromSleep(String reason) {
        if (!model.startWakeMotion()) {
            return;
        }

        pendingWakeReason = reason;
        currentState = CharacterState.WAKING;

        LAppMinimumPal.printLog(
                "[APP] state changed: SLEEP -> WAKING ("
                        + reason
                        + ")"
        );
    }

    private void finishWake() {
        model.finishWakeMotion();
        model.setIdleEffectsEnabled(true);

        currentState = CharacterState.IDLE;
        inactivityElapsedSeconds = 0.0f;
        sleepElapsedSeconds = 0.0f;
        boredPlayedForCurrentInactivity = false;

        LAppMinimumPal.printLog(
                "[APP] state changed: WAKING -> IDLE ("
                        + pendingWakeReason
                        + ")"
        );
        pendingWakeReason = "";
    }

    public boolean onUserActivity() {
        if (currentState == CharacterState.FIRST_VISIT) {
            return true;
        }

        inactivityElapsedSeconds = 0.0f;
        boredPlayedForCurrentInactivity = false;

        if (currentState == CharacterState.BORED) {
            model.stopBoredMotion();
            currentState = CharacterState.IDLE;

            LAppMinimumPal.printLog(
                    "[APP] state changed: BORED -> IDLE (user activity)"
            );
            return true;
        } else if (currentState == CharacterState.SLEEP) {
            wakeUpFromSleep("user activity");
            return true;
        }

        return false;
    }

    public boolean canStartHeadInteraction() {
        return currentState == CharacterState.IDLE
                || currentState == CharacterState.HEAD_PAT
                || currentState == CharacterState.HEAD_DOUBLE_TAP;
    }

    public void onHeadPat(float patX, float patY) {
        if (model == null) {
            return;
        }

        if (currentState == CharacterState.HEAD_DOUBLE_TAP) {
            model.stopHeadDoubleTapMotion();
            currentState = CharacterState.IDLE;
        }

        if (currentState == CharacterState.HEAD_PAT
                && model.isHeadPatReleasing()) {
            if (!model.startHeadPatMotion()) {
                return;
            }
        }

        if (currentState == CharacterState.IDLE) {
            if (!model.startHeadPatMotion()) {
                return;
            }

            currentState = CharacterState.HEAD_PAT;
            inactivityElapsedSeconds = 0.0f;
            boredPlayedForCurrentInactivity = false;
            model.setIdleEffectsEnabled(true);

            LAppMinimumPal.printLog(
                    "[APP] state changed: IDLE -> HEAD_PAT"
            );
        }

        if (currentState == CharacterState.HEAD_PAT) {
            model.updateHeadPat(patX, patY);
        }
    }

    public void onHeadPatEnd() {
        if (currentState != CharacterState.HEAD_PAT) {
            return;
        }

        model.endHeadPatMotion();
    }

    public void cancelHeadPat() {
        if (currentState != CharacterState.HEAD_PAT) {
            return;
        }

        model.stopHeadPatMotion();
        currentState = CharacterState.IDLE;
        inactivityElapsedSeconds = 0.0f;

        LAppMinimumPal.printLog(
                "[APP] state changed: HEAD_PAT -> IDLE (cancel)"
        );
    }

    public void onHeadDoubleTap() {
        if (model == null || currentState != CharacterState.IDLE) {
            return;
        }

        if (!model.startHeadDoubleTapMotion()) {
            return;
        }

        currentState = CharacterState.HEAD_DOUBLE_TAP;
        inactivityElapsedSeconds = 0.0f;
        boredPlayedForCurrentInactivity = false;

        LAppMinimumPal.printLog(
                "[APP] state changed: IDLE -> HEAD_DOUBLE_TAP"
        );
    }

    /**
     * 画面をドラッグした時の処理
     *
     * @param x 画面のx座標
     * @param y 画面のy座標
     */
    public void onDrag(float x, float y) {
        model.setDragging(x, y);
    }

    public void onPinch(
            float gestureScale,
            float previousCenterX,
            float previousCenterY,
            float currentCenterX,
            float currentCenterY
    ) {
        if (gestureScale <= 0.0f) {
            return;
        }

        float oldScale = userScale;
        float newScale = oldScale * gestureScale;

        if (newScale < MIN_USER_SCALE) {
            newScale = MIN_USER_SCALE;
        }

        if (newScale > MAX_USER_SCALE) {
            newScale = MAX_USER_SCALE;
        }

        float scaleRatio = newScale / oldScale;

        userOffsetX =
                currentCenterX
                - scaleRatio * (previousCenterX - userOffsetX);

        userOffsetY =
                currentCenterY
                - scaleRatio * (previousCenterY - userOffsetY);

        userScale = newScale;

        cancelHeadPat();
    }

    /**
     * 現在のシーンで保持しているモデルを返す
     *
     * @param number モデルリストのインデックス値
     * @return モデルのインスタンスを返す。インデックス値が範囲外の場合はnullを返す
     */
    public LAppMinimumModel getModel(int number) {
        return model;
    }

    /**
     * モデルのオフスクリーンのサイズを設定する。
     *
     * @param width  ウィンドウの幅
     * @param height ウィンドウの高さ
     */
    public void setRenderTargetSize(int width, int height) {
        if (model != null) {
            model.setRenderTargetSize(width, height);
        }
    }

    /**
     * シングルトンインスタンス
     */
    private static LAppMinimumLive2DManager s_instance;

    private LAppMinimumLive2DManager() {
        loadModel("Mk6");
    }

    private LAppMinimumModel model;

    private final CubismMatrix44 viewMatrix = CubismMatrix44.create();
    private final CubismMatrix44 projection = CubismMatrix44.create();
    private float userScale = 1.0f;

    private static final float MIN_USER_SCALE = 0.4f;
    private static final float MAX_USER_SCALE = 2.6f;

    private float userOffsetX = 0.0f;
    private float userOffsetY = 0.0f;

    private CharacterState currentState = CharacterState.LOADING;
    private PetPreferences petPreferences;
    private float inactivityElapsedSeconds = 0.0f;
    private float sleepElapsedSeconds = 0.0f;
    private boolean boredPlayedForCurrentInactivity = false;
    private String pendingWakeReason = "";

    private static final float BORED_AFTER_SECONDS = 5.0f * 60.0f;
    private static final float SLEEP_AFTER_SECONDS = 8.0f * 60.0f;
    private static final float SLEEP_DURATION_SECONDS = 8.0f * 60.0f;
}

