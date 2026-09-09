package com.sihoo.ai_agent_virtual_mobile.live2D;

import com.sihoo.ai_agent_virtual_mobile.character.CharacterMotionPlayer;
import com.sihoo.ai_agent_virtual_mobile.character.OutfitType;

final class Live2DCharacterMotionPlayer implements CharacterMotionPlayer {
    private LAppMinimumModel model;

    void setModel(LAppMinimumModel model) {
        this.model = model;
    }

    @Override
    public boolean hasModel() {
        return model != null;
    }

    @Override
    public void setIdleEffectsEnabled(boolean enabled) {
        if (model != null) {
            model.setIdleEffectsEnabled(enabled);
        }
    }

    @Override
    public void clearFaceExpression() {
        if (model != null) {
            model.clearExpression();
        }
    }

    @Override
    public boolean applyOutfitVisual(OutfitType outfitType) {
        if (model == null || outfitType == null) {
            return false;
        }

        switch (outfitType) {
            case OUTFIT:
                return model.setOutfitExpression("Outfit");
            case JACKET_OFF:
                return model.setOutfitExpression("JaketOFF");
            case DEFAULT:
                return model.clearOutfitExpression();
            default:
                return false;
        }
    }

    @Override
    public boolean startAppearanceMotion() {
        if (model == null) {
            return false;
        }
        model.startAppearanceMotion();
        return true;
    }

    @Override
    public boolean isAppearanceMotionFinished() {
        return model != null && model.isFirstVisitMotionFinished();
    }

    @Override
    public boolean startBoredMotion() {
        return model != null && model.startBoredMotion();
    }

    @Override
    public void stopBoredMotion() {
        if (model != null) {
            model.stopBoredMotion();
        }
    }

    @Override
    public boolean isBoredMotionFinished() {
        return model == null || model.isBoredMotionFinished();
    }

    @Override
    public boolean startSleepEntryMotion() {
        return model != null && model.startSleepEntryMotion();
    }

    @Override
    public boolean isSleepEntryFinished() {
        return model == null || model.isSleepEntryFinished();
    }

    @Override
    public boolean startSleepLoopMotion() {
        return model != null && model.startSleepLoopMotion();
    }

    @Override
    public boolean startWakeMotion() {
        return model != null && model.startWakeMotion();
    }

    @Override
    public boolean isWakeMotionFinished() {
        return model == null || model.isWakeMotionFinished();
    }

    @Override
    public void finishWakeMotion() {
        if (model != null) {
            model.finishWakeMotion();
        }
    }

    @Override
    public boolean startHeadPatMotion() {
        return model != null && model.startHeadPatMotion();
    }

    @Override
    public void updateHeadPat(float patX, float patY) {
        if (model != null) {
            model.updateHeadPat(patX, patY);
        }
    }

    @Override
    public void endHeadPatMotion() {
        if (model != null) {
            model.endHeadPatMotion();
        }
    }

    @Override
    public void stopHeadPatMotion() {
        if (model != null) {
            model.stopHeadPatMotion();
        }
    }

    @Override
    public boolean isHeadPatFinished() {
        return model == null || model.isHeadPatFinished();
    }

    @Override
    public boolean isHeadPatReleasing() {
        return model != null && model.isHeadPatReleasing();
    }

    @Override
    public boolean startHeadDoubleTapMotion() {
        return model != null && model.startHeadDoubleTapMotion();
    }

    @Override
    public void stopHeadDoubleTapMotion() {
        if (model != null) {
            model.stopHeadDoubleTapMotion();
        }
    }

    @Override
    public boolean isHeadDoubleTapFinished() {
        return model == null || model.isHeadDoubleTapFinished();
    }

    @Override
    public boolean startBodyStrokeMotion(boolean chest) {
        return model != null && model.startBodyStrokeMotion(chest);
    }

    @Override
    public void updateBodyStroke(float strokeX, float strokeY) {
        if (model != null) {
            model.updateBodyStroke(strokeX, strokeY);
        }
    }

    @Override
    public void endBodyStrokeMotion() {
        if (model != null) {
            model.endBodyStrokeMotion();
        }
    }

    @Override
    public void stopBodyStrokeMotion() {
        if (model != null) {
            model.stopBodyStrokeMotion();
        }
    }

    @Override
    public boolean isBodyStrokeFinished() {
        return model == null || model.isBodyStrokeFinished();
    }

    @Override
    public boolean isBodyStrokeReleasing() {
        return model != null && model.isBodyStrokeReleasing();
    }

    @Override
    public boolean startBodyDoubleTapMotion(boolean chest) {
        return model != null && model.startBodyDoubleTapMotion(chest);
    }

    @Override
    public void stopBodyDoubleTapMotion() {
        if (model != null) {
            model.stopBodyDoubleTapMotion();
        }
    }

    @Override
    public boolean isBodyDoubleTapFinished() {
        return model == null || model.isBodyDoubleTapFinished();
    }

    @Override
    public boolean startSurprisedExpression() {
        return model != null && model.setExpression("Surprised");
    }
}
