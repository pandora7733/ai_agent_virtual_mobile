package com.sihoo.ai_agent_virtual_mobile.live2D;

import com.sihoo.ai_agent_virtual_mobile.character.CharacterState;
import com.sihoo.ai_agent_virtual_mobile.character.CharacterStateController;
import com.sihoo.ai_agent_virtual_mobile.character.CharacterTimings;
import com.sihoo.ai_agent_virtual_mobile.character.OutfitType;
import com.sihoo.ai_agent_virtual_mobile.character.PetRepositories;
import com.sihoo.ai_agent_virtual_mobile.character.PetSession;
import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.rendering.android.CubismOffscreenManagerAndroid;

/**
 * Cubism 모델 로드·투영·그리기와 입력 좌표 변환을 담당한다.
 * 캐릭터 상태 전이는 {@link CharacterStateController}에 위임한다.
 */
public class LAppMinimumLive2DManager {
    public static LAppMinimumLive2DManager getInstance() {
        if (s_instance == null) {
            s_instance = new LAppMinimumLive2DManager();
        }
        return s_instance;
    }

    public static boolean hasInstance() {
        return s_instance != null;
    }

    public static void releaseInstance() {
        if (s_instance != null) {
            PetSession.unbind();
            if (s_instance.model != null) {
                s_instance.model.deleteModel();
                s_instance.model = null;
            }
            CubismOffscreenManagerAndroid.releaseInstance();
        }
        s_instance = null;
    }

    public boolean applyOutfit(OutfitType outfitType) {
        return characterController.applyOutfit(outfitType);
    }

    public boolean applyOutfit(OutfitType outfitType, boolean force) {
        return characterController.applyOutfit(outfitType, force);
    }

    public OutfitType getCurrentOutfit() {
        return characterController.getCurrentOutfit();
    }

    public void loadModel(String modelDirectoryName) {
        String dir = modelDirectoryName + "/";
        model = new LAppMinimumModel(dir);
        model.loadAssets(dir, modelDirectoryName + ".model3.json");
        motionPlayer.setModel(model);
        characterController.onModelReady();
    }

    public void onScreenHidden() {
        characterController.onScreenHidden();
    }

    public void onScreenShown() {
        LAppMinimumPal.updateTime();
        characterController.onScreenShown();
    }

    public void onUpdate() {
        int width = LAppMinimumDelegate.getInstance().getWindowWidth();
        int height = LAppMinimumDelegate.getInstance().getWindowHeight();
        float aspectRatio = (float) width / (float) height;
        float displayRatio = (float) height / (float) width;

        CubismOffscreenManagerAndroid.getInstance().beginFrameProcess();

        projection.loadIdentity();

        float canvasRatio = model.getModel().getCanvasHeight()
                / model.getModel().getCanvasWidth();
        final float finalModelSize = 2.0f * userScale;

        if (canvasRatio < displayRatio) {
            model.getModelMatrix().setWidth(finalModelSize);
            projection.scale(1.0f, aspectRatio);
        } else {
            model.getModelMatrix().setHeight(finalModelSize);
            projection.scale(1.0f / aspectRatio, 1.0f);
        }

        model.getModelMatrix().setPosition(userOffsetX, userOffsetY);

        float deltaTime = Math.max(
                0.0f,
                Math.min(LAppMinimumPal.getDeltaTime(), 0.1f)
        );
        characterController.update(deltaTime);

        if (viewMatrix != null) {
            viewMatrix.multiplyByMatrix(projection);
        }

        LAppMinimumDelegate.getInstance().getView().preModelDraw(model);

        model.update();
        model.draw(projection);

        LAppMinimumDelegate.getInstance().getView().postModelDraw(model);

        CubismOffscreenManagerAndroid.getInstance().endFrameProcess();
        CubismOffscreenManagerAndroid.getInstance().releaseStaleRenderTextures();
    }

    public boolean onUserActivity() {
        return characterController.onUserActivity();
    }

    public CharacterState getCurrentState() {
        return characterController.getCurrentState();
    }

    public boolean canStartHeadInteraction() {
        return characterController.canStartInteraction();
    }

    public boolean canStartBodyInteraction() {
        return characterController.canStartInteraction();
    }

    public void onHeadPat(float patX, float patY) {
        characterController.onHeadPat(patX, patY);
    }

    public void onHeadPatEnd() {
        characterController.onHeadPatEnd();
    }

    public void cancelHeadPat() {
        characterController.cancelHeadPat();
    }

    public void onHeadDoubleTap() {
        characterController.onHeadDoubleTap();
    }

    public void onBodyStroke(
            float strokeX,
            float strokeY,
            LAppMinimumView.HitRegion region
    ) {
        characterController.onBodyStroke(
                strokeX,
                strokeY,
                region == LAppMinimumView.HitRegion.CHEST
        );
    }

    public void onBodyStrokeEnd() {
        characterController.onBodyStrokeEnd();
    }

    public void cancelBodyStroke() {
        characterController.cancelBodyStroke();
    }

    public void onBodyDoubleTap(LAppMinimumView.HitRegion region) {
        characterController.onBodyDoubleTap(
                region == LAppMinimumView.HitRegion.CHEST
        );
    }

    public void onDrag(float x, float y) {
        if (model != null) {
            model.setDragging(x, y);
        }
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

        characterController.cancelHeadPat();
        characterController.cancelBodyStroke();
        characterController.cancelBodyDoubleTap();
    }

    public void cancelBodyDoubleTap() {
        characterController.cancelBodyDoubleTap();
    }

    public float getUserScale() {
        return userScale;
    }

    public float getUserOffsetX() {
        return userOffsetX;
    }

    public float getUserOffsetY() {
        return userOffsetY;
    }

    public LAppMinimumModel getModel(int number) {
        return model;
    }

    public void setRenderTargetSize(int width, int height) {
        if (model != null) {
            model.setRenderTargetSize(width, height);
        }
    }

    private static LAppMinimumLive2DManager s_instance;

    private LAppMinimumLive2DManager() {
        motionPlayer = new Live2DCharacterMotionPlayer();
        characterController = new CharacterStateController(
                motionPlayer,
                PetRepositories.get(),
                CharacterTimings.defaults()
        );
        PetSession.bind(characterController);
        loadModel("Mk6");
    }

    private LAppMinimumModel model;
    private final Live2DCharacterMotionPlayer motionPlayer;
    private final CharacterStateController characterController;
    private final CubismMatrix44 viewMatrix = CubismMatrix44.create();
    private final CubismMatrix44 projection = CubismMatrix44.create();
    private float userScale = 1.0f;

    private static final float MIN_USER_SCALE = 0.4f;
    private static final float MAX_USER_SCALE = 2.6f;

    private float userOffsetX = 0.0f;
    private float userOffsetY = 0.0f;
}
