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
            firstVisitElapsed = 0.0f;
            firstVisitFrameStarted = false;
            model.setExpression("Happy");
        } else {
            currentState = CharacterState.IDLE;
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

        if (currentState == CharacterState.FIRST_VISIT && petPreferences != null) {
            if (!firstVisitFrameStarted) {
                firstVisitFrameStarted = true;
            } else {
                float deltaTime = LAppMinimumPal.getDeltaTime();
                deltaTime = Math.min(deltaTime, 0.1f);
                firstVisitElapsed += deltaTime;
            }

            if (firstVisitElapsed >= FIRST_VISIT_DURATION) {
                model.clearExpression();

                petPreferences.markFirstVisitCompleted();

                currentState = CharacterState.IDLE;

                LAppMinimumPal.printLog(
                        "[APP] state changed: FIRST_VISIT -> IDLE"
                );
            }
        }

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
    private float firstVisitElapsed = 0.0f;
    private boolean firstVisitFrameStarted = false;
    private PetPreferences petPreferences;

    private static final float FIRST_VISIT_DURATION = 3.0f;
}

