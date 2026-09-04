/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at http://live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.sihoo.ai_agent_virtual_mobile.live2D;

import com.sihoo.ai_agent_virtual_mobile.live2D.demo.LAppDefine;
import com.live2d.sdk.cubism.framework.CubismDefaultParameterId;
import com.live2d.sdk.cubism.framework.CubismFramework;
import com.live2d.sdk.cubism.framework.CubismModelSettingJson;
import com.live2d.sdk.cubism.framework.ICubismModelSetting;
import com.live2d.sdk.cubism.framework.effect.CubismLook;
import com.live2d.sdk.cubism.framework.effect.CubismBreath;
import com.live2d.sdk.cubism.framework.id.CubismId;
import com.live2d.sdk.cubism.framework.id.CubismIdManager;
import com.live2d.sdk.cubism.framework.math.CubismMatrix44;
import com.live2d.sdk.cubism.framework.model.CubismMoc;
import com.live2d.sdk.cubism.framework.model.CubismUserModel;
import com.live2d.sdk.cubism.framework.motion.ACubismMotion;
import com.live2d.sdk.cubism.framework.motion.CubismExpressionMotion;
import com.live2d.sdk.cubism.framework.motion.CubismExpressionUpdater;
import com.live2d.sdk.cubism.framework.motion.CubismLookUpdater;
import com.live2d.sdk.cubism.framework.motion.CubismMotion;
import com.live2d.sdk.cubism.framework.motion.CubismPhysicsUpdater;
import com.live2d.sdk.cubism.framework.motion.CubismPoseUpdater;
import com.live2d.sdk.cubism.framework.motion.CubismBreathUpdater;
import com.live2d.sdk.cubism.framework.rendering.CubismRenderer;
import com.live2d.sdk.cubism.framework.rendering.android.CubismRenderTargetAndroid;
import com.live2d.sdk.cubism.framework.rendering.android.CubismRendererAndroid;
import com.live2d.sdk.cubism.framework.utils.CubismDebug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LAppMinimumModel extends CubismUserModel {
    public LAppMinimumModel(String modelDirName) {
        CubismIdManager idManager = CubismFramework.getIdManager();

        idParamAngleX = idManager.getId(CubismDefaultParameterId.ParameterId.ANGLE_X.getId());
        idParamAngleY = idManager.getId(CubismDefaultParameterId.ParameterId.ANGLE_Y.getId());
        idParamAngleZ = idManager.getId(CubismDefaultParameterId.ParameterId.ANGLE_Z.getId());
        idParamBodyAngleX = idManager.getId(CubismDefaultParameterId.ParameterId.BODY_ANGLE_X.getId());
        idParamEyeBallX = idManager.getId(CubismDefaultParameterId.ParameterId.EYE_BALL_X.getId());
        idParamEyeBallY = idManager.getId(CubismDefaultParameterId.ParameterId.EYE_BALL_Y.getId());
        idParamEyeLOpen = idManager.getId(CubismDefaultParameterId.ParameterId.EYE_L_OPEN.getId());
        idParamEyeROpen = idManager.getId(CubismDefaultParameterId.ParameterId.EYE_R_OPEN.getId());
        naturalBlinkController = new NaturalBlinkController(
                idParamEyeLOpen,
                idParamEyeROpen
        );
        idParamBreath = idManager.getId(CubismDefaultParameterId.ParameterId.BREATH.getId());

        firstVisitMotionController = new FirstVisitMotionController(
                idParamAngleX,
                idParamAngleY,
                idParamAngleZ,
                idParamBodyAngleX,
                idParamEyeBallX,
                idParamEyeBallY,
                idParamEyeLOpen,
                idParamEyeROpen
        );
        boredMotionController = new BoredMotionController(
                idParamAngleX,
                idParamAngleY,
                idParamAngleZ,
                idParamBodyAngleX
        );
        sleepEntryTransition = new ParameterTransitionController();
        wakeTransition = new ParameterTransitionController();

        modelHomeDirectory = modelDirName;
    }

    public void loadAssets(final String dir, final String fileName) {
        modelHomeDirectory = dir;
        String filePath = modelHomeDirectory + fileName;

        // Setup model
        setupModel(filePath);

        // Setup renderer.
        CubismRenderer renderer = CubismRendererAndroid.create(
            LAppMinimumDelegate.getInstance().getWindowWidth(),
            LAppMinimumDelegate.getInstance().getWindowHeight()
        );
        setupRenderer(renderer);

        setupTextures();
    }

    /**
     * Delete the model which LAppModel has.
     */
    public void deleteModel() {
        delete();
    }

    /**
     * レンダラとテクスチャを再構築する。GLコンテキストが破棄された場合に呼び出す。
     */
    public void reloadRenderer() {
        deleteRenderer();

        CubismRenderer renderer = CubismRendererAndroid.create(
            LAppMinimumDelegate.getInstance().getWindowWidth(),
            LAppMinimumDelegate.getInstance().getWindowHeight()
        );
        setupRenderer(renderer);

        setupTextures();
    }

    /**
     * モデルの更新処理。モデルのパラメーターから描画状態を決定する
     */
    public void update() {
        isUpdated(false);

        final float deltaTimeSeconds = LAppMinimumPal.getDeltaTime();
        _userTimeSeconds += deltaTimeSeconds;

        // モーションによるパラメーター更新の有無
        motionUpdated = false;

        // 前回セーブされた状態をロード
        model.loadParameters();

        // モーションの再生がない場合、待機モーションの中からランダムで再生する
        if (!isSleepTransitionActive()
                && motionManager.isFinished()) {
            final String idleGroup = LAppDefine.MotionGroup.IDLE.getId();

            // model3.json に Idle モーションが登録されていない場合は再生を試みない。
            if (!boredMotionController.isActive()
                    && !sleepMotionActive
                    && modelSetting.getMotionCount(idleGroup) > 0) {
                startMotion(idleGroup, 0, LAppDefine.Priority.IDLE.getPriority());
            }
        } else if (!isSleepTransitionActive()) {
            // モーションを更新
            motionUpdated = motionManager.updateMotion(model, deltaTimeSeconds);
        }

        sleepEntryTransition.update(model, deltaTimeSeconds);
        wakeTransition.update(model, deltaTimeSeconds);

        firstVisitMotionController.update(
                model,
                deltaTimeSeconds
        );

        if (!isSleepTransitionActive()) {
            boredMotionController.update(
                    model,
                    deltaTimeSeconds
            );
        }

        if (idleEffectsEnabled) {
            naturalBlinkController.update(
                    model,
                    deltaTimeSeconds
            );
        }

        // モデルの状態を保存
        model.saveParameters();

        // 各種パラメーターの更新（表情・物理・ポーズ・ドラッグ追従など）
        updateScheduler.onLateUpdate(model, deltaTimeSeconds);

        model.update();

        isUpdated(true);
    }

    public void setIdleEffectsEnabled(boolean enabled) {
        idleEffectsEnabled = enabled;

        if (enabled) {
            naturalBlinkController.reset();
        }
    }

    public void startFirstVisitMotion() {
        firstVisitMotionController.start(model);
    }

    public boolean isFirstVisitMotionFinished() {
        return firstVisitMotionController.isFinished();
    }

    public boolean startBoredMotion() {
        motionManager.stopAllMotions();
        return boredMotionController.start(model);
    }

    public void stopBoredMotion() {
        boredMotionController.stop(model);
    }

    public boolean isBoredMotionFinished() {
        return boredMotionController.isFinished();
    }

    /**
     * Idle → Sleep 진입 전환 (기본 1.8초).
     * {@link #SLEEP_ENTRY_DURATION_SECONDS} 값을 수정해 조절한다.
     */
    public boolean startSleepEntryMotion() {
        motionManager.stopAllMotions();
        boredMotionController.stop(model);
        sleepMotionActive = false;

        capturePreSleepPose();

        float[] sleepTargetPose = buildSleepTargetPose();
        return sleepEntryTransition.start(
                model,
                sleepTargetPose,
                SLEEP_ENTRY_DURATION_SECONDS
        );
    }

    public boolean isSleepEntryFinished() {
        return sleepEntryTransition.isFinished();
    }

    public boolean startSleepLoopMotion() {
        if (sleepMotionActive) {
            return true;
        }

        final String sleepGroup = LAppDefine.MotionGroup.SLEEP.getId();
        if (modelSetting.getMotionCount(sleepGroup) <= 0) {
            return false;
        }

        motionManager.stopAllMotions();

        final String motionName = sleepGroup + "_0";
        CubismMotion sleepMotion = (CubismMotion) motions.get(motionName);
        if (sleepMotion != null) {
            // 루프 시작 시점을 눈이 감긴 이후(0.7초)로 맞춰 SLEEP_ENTRY 종료 자세와 일치시킨다.
            sleepMotion.setOffsetTime(SLEEP_LOOP_START_OFFSET_SECONDS);
            sleepMotion.setFadeInTime(0.0f);
        }

        applySleepSteadyPoseToModel();
        model.saveParameters();

        int motionId = startMotion(
                sleepGroup,
                0,
                LAppDefine.Priority.FORCE.getPriority()
        );

        sleepMotionActive = motionId != -1;
        return sleepMotionActive;
    }

    /**
     * Sleep → Idle 기상 전환 (기본 1.3초).
     * {@link #WAKE_DURATION_SECONDS} 값을 수정해 조절한다.
     */
    public boolean startWakeMotion() {
        if (preSleepParameterValues == null) {
            return false;
        }

        motionManager.stopAllMotions();
        sleepMotionActive = false;

        return wakeTransition.start(
                model,
                preSleepParameterValues,
                WAKE_DURATION_SECONDS
        );
    }

    public boolean isWakeMotionFinished() {
        return wakeTransition.isFinished();
    }

    public void finishWakeMotion() {
        wakeTransition.stop(model);
        model.saveParameters();
        preSleepParameterValues = null;
    }

    public boolean isSleepTransitionActive() {
        return sleepEntryTransition.isActive()
                || wakeTransition.isActive();
    }

    /** Idle → Sleep 진입 전환 시간[초] */
    public static final float SLEEP_ENTRY_DURATION_SECONDS = 1.8f;

    /** Sleep → Idle 기상 전환 시간[초] */
    public static final float WAKE_DURATION_SECONDS = 1.3f;

    /**
     * Sleep 루프 모션 재생 시작 위치[초].
     * Sleep.motion3.json에서 눈이 감기고 표정이 안정된 이후 시점.
     */
    private static final float SLEEP_LOOP_START_OFFSET_SECONDS = 0.7f;

    public boolean startSleepMotion() {
        return startSleepLoopMotion();
    }

    public void stopSleepMotion() {
        motionManager.stopAllMotions();
        sleepMotionActive = false;
        sleepEntryTransition.stop(model);
        wakeTransition.stop(model);
    }

    private float[] buildSleepTargetPose() {
        int parameterCount = model.getParameterCount();
        float[] targetPose = new float[parameterCount];

        for (int i = 0; i < parameterCount; i++) {
            targetPose[i] = model.getParameterValue(i);
        }

        applySleepSteadyPose(targetPose);
        return targetPose;
    }

    private void applySleepSteadyPoseToModel() {
        int parameterCount = model.getParameterCount();
        float[] targetPose = new float[parameterCount];

        for (int i = 0; i < parameterCount; i++) {
            targetPose[i] = model.getParameterValue(i);
        }

        applySleepSteadyPose(targetPose);

        for (int i = 0; i < parameterCount; i++) {
            model.setParameterValue(i, targetPose[i]);
        }
    }

    /**
     * Sleep.motion3.json 루프 안정 구간(0.7초 이후)의 파라미터 값.
     * SLEEP_ENTRY 목표 자세와 Sleep 루프 시작 프레임을 동일하게 맞춘다.
     */
    private void applySleepSteadyPose(float[] values) {
        CubismIdManager idManager = CubismFramework.getIdManager();

        setParameterInArray(values, idParamAngleX, -2.0f);
        setParameterInArray(values, idParamAngleY, 6.0f);
        setParameterInArray(values, idParamAngleZ, 19.0f);
        setParameterInArray(values, idParamBodyAngleX, -1.0f);
        setParameterInArray(values, idParamEyeLOpen, 0.0f);
        setParameterInArray(values, idParamEyeROpen, 0.0f);

        setParameterInArray(values, idManager.getId("BodyAngleY"), 1.0f);
        setParameterInArray(values, idManager.getId("ParamBodyAngleZ"), 10.0f);
        setParameterInArray(values, idManager.getId("ParamJawOpen"), 1.0f);
        setParameterInArray(values, idManager.getId("ParamMode2Form"), 1.0f);
        setParameterInArray(values, idManager.getId("ExpHairSticker"), 1.0f);
        setParameterInArray(values, idManager.getId("ExpMask"), 1.0f);
        setParameterInArray(values, idManager.getId("ParamMouthMode"), 1.0f);
        setParameterInArray(values, idManager.getId("ExpSleeping"), 1.0f);
        setParameterInArray(values, idManager.getId("ExpMouthSolidcolor"), 1.0f);
        setParameterInArray(values, idManager.getId("ParamMouthOpenY"), 0.5f);

        setParameterInArray(values, idManager.getId("ParamBrowLDown"), 0.0f);
        setParameterInArray(values, idManager.getId("ParamBrowRDown"), 0.0f);
        setParameterInArray(values, idManager.getId("ParamBrowLOutterUp"), 0.0f);
        setParameterInArray(values, idManager.getId("BrowROutterUp"), 0.0f);
        setParameterInArray(values, idManager.getId("BrowRInnerUp"), 0.0f);
        setParameterInArray(values, idManager.getId("ParamBrowForm"), 0.0f);
        setParameterInArray(values, idManager.getId("ParamMouthForm"), 0.0f);
        setParameterInArray(values, idManager.getId("ParamMouthFunnel"), 0.0f);
        setParameterInArray(values, idManager.getId("ParamMouthPressLipOpen"), 0.0f);
        setParameterInArray(values, idManager.getId("ParamMouthPunkerWiden"), 0.0f);
        setParameterInArray(values, idManager.getId("ParamMouthShrug"), 0.0f);
        setParameterInArray(values, idManager.getId("ParamMouthX"), 0.0f);
        setParameterInArray(values, idManager.getId("ParamCheekPuff"), 0.0f);
    }

    private void setParameterInArray(
            float[] values,
            CubismId parameterId,
            float value
    ) {
        int index = model.getParameterIndex(parameterId);

        if (index >= 0 && index < values.length) {
            values[index] = value;
        }
    }

    private void capturePreSleepPose() {
        int parameterCount = model.getParameterCount();
        preSleepParameterValues = new float[parameterCount];

        for (int i = 0; i < parameterCount; i++) {
            preSleepParameterValues[i] = model.getParameterValue(i);
        }
    }

    /**
     * 引数で指定したモーションの再生を開始する。
     *
     * @param group    モーショングループ名
     * @param number   グループ内の番号
     * @param priority 優先度
     * @return 開始したモーションの識別番号を返す。個別のモーションが終了したか否かを判別するisFinished()の引数で使用する。開始できない時は「-1」
     */
    public int startMotion(final String group, int number, int priority) {
        if (priority == LAppDefine.Priority.FORCE.getPriority()) {
            motionManager.setReservationPriority(priority);
        } else if (!motionManager.reserveMotion(priority)) {
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                CubismFramework.coreLogFunction("[APP] cannot start motion.");
            }
            return -1;
        }

        final String fileName = modelSetting.getMotionFileName(group, number);

        // ex) idle_0
        String name = group + "_" + number;

        CubismMotion motion = (CubismMotion) motions.get(name);

        if (motion == null) {
            if (!fileName.equals("")) {
                String path = modelHomeDirectory + fileName;

                byte[] buffer;
                buffer = LAppMinimumPal.loadFileAsBytes(path);

                CubismMotion tmpMotion = loadMotion(buffer);
                if (tmpMotion != null) {
                    motion = (CubismMotion) tmpMotion;

                    float fadeInTime = modelSetting.getMotionFadeInTimeValue(group, number);
                    if (fadeInTime != -1.0f) {
                        motion.setFadeInTime(fadeInTime);
                    }

                    float fadeOutTime = modelSetting.getMotionFadeOutTimeValue(group, number);
                    if (fadeOutTime != -1.0f) {
                        motion.setFadeOutTime(fadeOutTime);
                    }
                }
            }
        }

        if (LAppDefine.DEBUG_LOG_ENABLE) {
            CubismFramework.coreLogFunction("[APP] start motion: " + group + "_" + number);
        }

        return motionManager.startMotionPriority(motion, priority);
    }

    public boolean setExpression(String expressionName) {
        ACubismMotion expression = expressions.get(expressionName);

        if (!(expression instanceof CubismExpressionMotion)) {
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                CubismFramework.coreLogFunction(
                        "[APP] expression not found: " + expressionName
                );
            }
            return false;
        }

        int motionId = expressionManager.startMotionPriority(
                expression,
                LAppDefine.Priority.NORMAL.getPriority()
        );

        if (LAppDefine.DEBUG_LOG_ENABLE) {
            CubismFramework.coreLogFunction(
                    "[APP] start expression: "
                            + expressionName
                            + ", id="
                            + motionId
            );
        }

        return motionId != -1;
    }

    public boolean clearExpression() {
        expressionManager.stopAllMotions();
        return true;
    }

    public void draw(CubismMatrix44 matrix) {
        if (model == null) {
            LAppMinimumDelegate.getInstance().getActivity().finish();
        }

        // キャッシュ変数の定義を避けるために、multiplyByMatrix()ではなく、multiply()を使用する。
        CubismMatrix44.multiply(
            modelMatrix.getArray(),
            matrix.getArray(),
            matrix.getArray()
        );

        this.<CubismRendererAndroid>getRenderer().setMvpMatrix(matrix);
        this.<CubismRendererAndroid>getRenderer().drawModel();
    }

    public CubismRenderTargetAndroid getRenderingBuffer() {
        return renderingBuffer;
    }

    /**
     * .moc3ファイルの整合性をチェックする。
     *
     * @param mocFileName MOC3ファイル名
     * @return MOC3に整合性があるかどうか。整合性があればtrue。
     */
    public boolean hasMocConsistencyFromFile(String mocFileName) {
        assert mocFileName != null && !mocFileName.isEmpty();

        String path = mocFileName;
        path = modelHomeDirectory + path;

        byte[] buffer = LAppMinimumPal.loadFileAsBytes(path);
        boolean consistency = CubismMoc.hasMocConsistency(buffer);

        if (!consistency) {
            CubismDebug.cubismLogInfo("Inconsistent MOC3.");
        } else {
            CubismDebug.cubismLogInfo("Consistent MOC3.");
        }

        return consistency;
    }

    // model3.jsonからモデルを生成する
    private boolean setupModel(String model3JsonPath) {
        byte[] model3Json = LAppMinimumPal.loadFileAsBytes(model3JsonPath);

        CubismModelSettingJson modelSetting = null;
        modelSetting = new CubismModelSettingJson(model3Json);

        if (modelSetting != null) {
            this.modelSetting = modelSetting;
        }

        // model3.jsonが上手く読み込まれていない場合終了する
        if (this.modelSetting.getJson() == null) {
            if (LAppDefine.DEBUG_LOG_ENABLE) {
                CubismFramework.coreLogFunction("[ERROR]model3.json is not found");
            }
            LAppMinimumDelegate.getInstance().getActivity().finish();
        }

        // Load Cubism Model
        {
            String path = this.modelSetting.getModelFileName();
            if (!path.equals("")) {
                String modelPath = modelHomeDirectory + path;
                byte[] buffer = LAppMinimumPal.loadFileAsBytes(modelPath);

                loadModel(buffer, mocConsistency);
            }
        }

        // load expression files(.exp3.json)
        // 表情モーションの読み込み
        if (this.modelSetting.getExpressionCount() > 0) {
            final int count = this.modelSetting.getExpressionCount();

            for (int i = 0; i < count; i++) {
                String name = this.modelSetting.getExpressionName(i);

                String path = this.modelSetting.getExpressionFileName(i);
                String modelPath = modelHomeDirectory + path;

                byte[] buffer = LAppMinimumPal.loadFileAsBytes(modelPath);

                CubismExpressionMotion motion = loadExpression(buffer);

                expressions.put(name, motion);
            }

            updateScheduler.addUpdatableList(new CubismExpressionUpdater(expressionManager));
        }

        // Pose
        {
            String path = this.modelSetting.getPoseFileName();
            if (!path.equals("")) {
                String modelPath = modelHomeDirectory + path;

                byte[] buffer = LAppMinimumPal.loadFileAsBytes(modelPath);

                loadPose(buffer);
            }
            if (pose != null) {
                updateScheduler.addUpdatableList(new CubismPoseUpdater(pose));
            }
        }

        // Physics
        {
            String path = this.modelSetting.getPhysicsFileName();
            if (!path.equals("")) {
                String modelPath = modelHomeDirectory + path;
                byte[] buffer = LAppMinimumPal.loadFileAsBytes(modelPath);

                loadPhysics(buffer);
            }
            if (physics != null) {
                updateScheduler.addUpdatableList(new CubismPhysicsUpdater(physics));
            }
        }

        // Load UserData
        {
            String path = this.modelSetting.getUserDataFile();
            if (!path.equals("")) {
                String modelPath = modelHomeDirectory + path;
                byte[] buffer = LAppMinimumPal.loadFileAsBytes(modelPath);

                loadUserData(buffer);
            }
        }

        // Look
        {
            look = CubismLook.create();

            List<CubismLook.LookParameterData> lookParameters = new ArrayList<CubismLook.LookParameterData>();
            lookParameters.add(new CubismLook.LookParameterData(idParamAngleX, 30.0f));
            lookParameters.add(new CubismLook.LookParameterData(idParamAngleY, 0.0f, 30.0f));
            lookParameters.add(new CubismLook.LookParameterData(idParamAngleZ, 0.0f, 0.0f, -30.0f));
            lookParameters.add(new CubismLook.LookParameterData(idParamBodyAngleX, 10.0f));
            lookParameters.add(new CubismLook.LookParameterData(idParamEyeBallX, 1.0f));
            lookParameters.add(new CubismLook.LookParameterData(idParamEyeBallY, 0.0f, 1.0f));

            look.setParameters(lookParameters);

            updateScheduler.addUpdatableList(new CubismLookUpdater(look, dragManager));
        }

        breath = CubismBreath.create();

        List<CubismBreath.BreathParameterData> breathParameters =
                new ArrayList<>();

//      peak: 움직임 크기
//      weight: 적용 강도
//      cycle: 호흡 속도
//      offset: 기본 위치 보정

//      고개 상하
        breathParameters.add(
                new CubismBreath.BreathParameterData(
                        idParamAngleY,
                        0.0f,
                        2.5f,
                        4.5f,
                        1.50f
                )
        );

//      고개 기울기
        breathParameters.add(
                new CubismBreath.BreathParameterData(
                        idParamAngleZ,
                        0.0f,
                        1.2f,
                        4.8f,
                        0.85f
                )
        );
//      몸 움직임
        breathParameters.add(
                new CubismBreath.BreathParameterData(
                        idParamBodyAngleX,
                        0.0f,
                        1.2f,
                        4.2f,
                        0.7f
                )
        );

//      모델 내부 호흡 변형
        breathParameters.add(
                new CubismBreath.BreathParameterData(
                        idParamBreath,
                        0.0f,
                        0.7f,
                        3.7f,
                        1.1f
                )
        );

        breath.setParameters(breathParameters);

        updateScheduler.addUpdatableList(
                new CubismBreathUpdater(breath)
        );

        updateScheduler.sortUpdatableList();


        // Set layout
        Map<String, Float> layout = new HashMap<String, Float>();
        this.modelSetting.getLayoutMap(layout);

        // If layout information exists, the model matrix is set up from it.
        if (this.modelSetting.getLayoutMap(layout)) {
            modelMatrix.setupFromLayout(layout);
        }

        model.saveParameters();

        // Load motions
        for (int i = 0; i < modelSetting.getMotionGroupCount(); i++) {
            String group = modelSetting.getMotionGroupName(i);
            preLoadMotionGroup(group);
        }

        motionManager.stopAllMotions();

        return true;
    }

    /**
     * モーションデータをグループ名から一括でロードする。
     * モーションデータの名前はModelSettingから取得する。
     *
     * @param group モーションデータのグループ名
     **/
    private void preLoadMotionGroup(final String group) {
        final int count = modelSetting.getMotionCount(group);

        for (int i = 0; i < count; i++) {
            // ex) idle_0
            String name = group + "_" + i;

            String path = modelSetting.getMotionFileName(group, i);
            if (!path.equals("")) {
                String modelPath = modelHomeDirectory + path;

                if (LAppDefine.DEBUG_LOG_ENABLE) {
                    CubismFramework.coreLogFunction("[APP]load motion: " + path + " ==>[" + group + "_" + i + "]");
                }

                byte[] buffer;
                buffer = LAppMinimumPal.loadFileAsBytes(modelPath);

                CubismMotion tmp = loadMotion(buffer);
                if (tmp == null) {
                    continue;
                }
                CubismMotion motion = tmp;

                final float fadeInTime = modelSetting.getMotionFadeInTimeValue(group, i);
                if (fadeInTime != -1.0f) {
                    motion.setFadeInTime(fadeInTime);
                }

                final float fadeOutTime = modelSetting.getMotionFadeOutTimeValue(group, i);
                if (fadeOutTime != -1.0f) {
                    motion.setFadeOutTime(fadeOutTime);
                }

                motion.setEffectIds(eyeBlinkIds, lipSyncIds);

                motions.put(name, motion);
            }
        }
    }

    /**
     * OpenGLのテクスチャユニットにテクスチャをロードする
     */
    private void setupTextures() {
        for (int modelTextureNumber = 0; modelTextureNumber < modelSetting.getTextureCount(); modelTextureNumber++) {
            // テクスチャ名が空文字だった場合はロード・バインド処理をスキップ
            if (modelSetting.getTextureFileName(modelTextureNumber).equals("")) {
                continue;
            }

            // OpenGL ESのテクスチャユニットにテクスチャをロードする
            String texturePath = modelSetting.getTextureFileName(modelTextureNumber);
            texturePath = modelHomeDirectory + texturePath;

            LAppMinimumTextureManager.TextureInfo texture =
                LAppMinimumDelegate.getInstance()
                    .getTextureManager()
                    .createTextureFromPngFile(texturePath);
            final int glTextureNumber = texture.id;

            ((CubismRendererAndroid) getRenderer()).bindTexture(modelTextureNumber, glTextureNumber);

            if (LAppDefine.PREMULTIPLIED_ALPHA_ENABLE) {
                this.<CubismRendererAndroid>getRenderer().isPremultipliedAlpha(true);
            } else {
                this.<CubismRendererAndroid>getRenderer().isPremultipliedAlpha(false);
            }
        }
    }

    private ICubismModelSetting modelSetting;
    /**
     * モデルのホームディレクトリ
     */
    private String modelHomeDirectory;
    /**
     * デルタ時間の積算値[秒]
     */
    private float _userTimeSeconds;

    private final List<CubismId> eyeBlinkIds = new ArrayList<CubismId>();
    private final List<CubismId> lipSyncIds = new ArrayList<CubismId>();
    /**
     * 読み込まれているモーションのマップ
     */
    private final Map<String, ACubismMotion> motions = new HashMap<String, ACubismMotion>();
    /**
     * 読み込まれている表情のマップ
     */
    private final Map<String, ACubismMotion> expressions = new HashMap<String, ACubismMotion>();

    /**
     * パラメーターID: ParamAngleX
     */
    private final CubismId idParamAngleX;
    /**
     * パラメーターID: ParamAngleY
     */
    private final CubismId idParamAngleY;
    /**
     * パラメーターID: ParamAngleZ
     */
    private final CubismId idParamAngleZ;
    /**
     * パラメーターID: ParamBodyAngleX
     */
    private final CubismId idParamBodyAngleX;
    /**
     * パラメーターID: ParamEyeBallX
     */
    private final CubismId idParamEyeBallX;
    /**
     * パラメーターID: ParamEyeBallY
     */
    private final CubismId idParamEyeBallY;
    private final CubismId idParamEyeLOpen;
    private final CubismId idParamEyeROpen;
    private final CubismId idParamBreath;
    private final FirstVisitMotionController firstVisitMotionController;
    private final BoredMotionController boredMotionController;
    private final NaturalBlinkController naturalBlinkController;
    private final ParameterTransitionController sleepEntryTransition;
    private final ParameterTransitionController wakeTransition;
    private float[] preSleepParameterValues;
    private boolean sleepMotionActive = false;
    private boolean idleEffectsEnabled = false;
    /**
     * 現フレームでメインモーションがパラメーターを更新したか
     */
    private boolean motionUpdated;

    /**
     * フレームバッファ以外の描画先
     */
    private CubismRenderTargetAndroid renderingBuffer = new CubismRenderTargetAndroid();
}
